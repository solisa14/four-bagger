package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

  @Mock private TournamentRepository tournamentRepository;

  @InjectMocks private TournamentService tournamentService;

  private User organizer() {
    return TestDataFactory.user(
        UUID.randomUUID(), "organizer", "org@example.com", "encoded", Role.USER);
  }

  private User player() {
    return TestDataFactory.user(
        UUID.randomUUID(), "player", "player@example.com", "encoded", Role.USER);
  }

  private Tournament registrationTournament() {
    return Tournament.builder()
        .id(UUID.randomUUID())
        .organizer(organizer())
        .title("Test Tournament")
        .status(TournamentStatus.REGISTRATION)
        .joinCode("ABC123")
        .build();
  }

  private TournamentParticipant participant(Tournament tournament) {
    return TournamentParticipant.builder()
        .id(UUID.randomUUID())
        .tournament(tournament)
        .user(player())
        .build();
  }

  private Tournament tournamentWithParticipants(TournamentStatus status, int participantCount) {
    Tournament tournament = registrationTournament();
    tournament.setStatus(status);
    for (int i = 0; i < participantCount; i++) {
      tournament.getParticipants().add(participant(tournament));
    }
    return tournament;
  }

  // --- removeParticipant ---
  @Test
  void removeParticipant_whenTournamentIsInRegistration_removesParticipant() {
    Tournament tournament = registrationTournament();
    TournamentParticipant participant =
        TournamentParticipant.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .user(player())
            .build();
    tournament.getParticipants().add(participant);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    tournamentService.removeParticipant(tournament.getId(), participant.getId());
    assertThat(tournament.getParticipants()).isEmpty();
    verify(tournamentRepository).save(tournament);
  }

  @Test
  void removeParticipant_whenTournamentHasMultipleParticipants_removesOnlyTargetParticipant() {
    Tournament tournament = registrationTournament();
    TournamentParticipant participantOne =
        TournamentParticipant.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .user(player())
            .build();
    TournamentParticipant participantTwo =
        TournamentParticipant.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .user(player())
            .build();
    tournament.getParticipants().add(participantOne);
    tournament.getParticipants().add(participantTwo);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));

    tournamentService.removeParticipant(tournament.getId(), participantOne.getId());

    assertThat(tournament.getParticipants()).containsExactly(participantTwo);
    verify(tournamentRepository).save(tournament);
  }

  @Test
  void removeParticipant_whenTournamentIsNotInRegistration_throwsException() {
    Tournament tournament = registrationTournament();
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    UUID participantId = UUID.randomUUID();
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    assertThatThrownBy(() -> tournamentService.removeParticipant(tournament.getId(), participantId))
        .isInstanceOf(InvalidTournamentStateException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  void removeParticipant_whenTournamentIsBracketReady_throwsException() {
    Tournament tournament = registrationTournament();
    tournament.setStatus(TournamentStatus.BRACKET_READY);
    UUID participantId = UUID.randomUUID();
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.removeParticipant(tournament.getId(), participantId))
        .isInstanceOf(InvalidTournamentStateException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  void removeParticipant_whenParticipantDoesNotExist_throwsNotFoundException() {
    Tournament tournament = registrationTournament();
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    assertThatThrownBy(
            () -> tournamentService.removeParticipant(tournament.getId(), UUID.randomUUID()))
        .isInstanceOf(TournamentParticipantNotFoundException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  void removeParticipant_whenTournamentDoesNotExist_throwsNotFoundException() {
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> tournamentService.removeParticipant(tournamentId, participantId))
        .isInstanceOf(TournamentNotFoundException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  // --- generateBracket ---

  @Test
  void generateBracket_whenTournamentIsInRegistration_setsBracketReadyAndAssignsSeeds() {
    Tournament tournament = tournamentWithParticipants(TournamentStatus.REGISTRATION, 4);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

    tournamentService.generateBracket(tournament.getId());

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.BRACKET_READY);
    assertThat(tournament.getTeams()).hasSize(4);
    assertThat(tournament.getTeams())
        .extracting(TournamentTeam::getSeed)
        .containsExactlyInAnyOrder(1, 2, 3, 4);
    assertThat(tournament.getTeams()).extracting(TournamentTeam::getPlayerTwo).containsOnlyNulls();
    verify(tournamentRepository).save(tournament);
  }

  @Test
  void generateBracket_whenTournamentIsAlreadyBracketReady_reshufflesAndReassignsSeeds() {
    Tournament tournament = tournamentWithParticipants(TournamentStatus.BRACKET_READY, 4);
    tournament
        .getParticipants()
        .forEach(
            p ->
                tournament
                    .getTeams()
                    .add(
                        TournamentTeam.builder()
                            .tournament(tournament)
                            .playerOne(p.getUser())
                            .build()));
    tournament.getTeams().get(0).setSeed(10);
    tournament.getTeams().get(1).setSeed(20);
    tournament.getTeams().get(2).setSeed(30);
    tournament.getTeams().get(3).setSeed(40);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

    tournamentService.generateBracket(tournament.getId());

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.BRACKET_READY);
    assertThat(tournament.getTeams())
        .extracting(TournamentTeam::getSeed)
        .containsExactlyInAnyOrder(1, 2, 3, 4);
    verify(tournamentRepository).save(tournament);
  }

  @Test
  void generateBracket_whenTournamentIsInProgress_throwsException() {
    Tournament tournament = tournamentWithParticipants(TournamentStatus.IN_PROGRESS, 4);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.generateBracket(tournament.getId()))
        .isInstanceOf(InvalidTournamentStateException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  void generateBracket_whenTournamentIsCompleted_throwsException() {
    Tournament tournament = tournamentWithParticipants(TournamentStatus.COMPLETED, 4);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.generateBracket(tournament.getId()))
        .isInstanceOf(InvalidTournamentStateException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  void generateBracket_whenTournamentHasTwoOrFewerParticipants_throwsException() {
    Tournament tournament = tournamentWithParticipants(TournamentStatus.REGISTRATION, 2);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.generateBracket(tournament.getId()))
        .isInstanceOf(InvalidTournamentStateException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  void generateBracket_whenTournamentHasThreeParticipants_assignsAllSeeds() {
    Tournament tournament = tournamentWithParticipants(TournamentStatus.REGISTRATION, 3);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

    tournamentService.generateBracket(tournament.getId());

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.BRACKET_READY);
    assertThat(tournament.getTeams())
        .extracting(TournamentTeam::getSeed)
        .containsExactlyInAnyOrder(1, 2, 3);
    verify(tournamentRepository).save(tournament);
  }

  @Test
  void generateBracket_whenTournamentDoesNotExist_throwsNotFoundException() {
    UUID tournamentId = UUID.randomUUID();
    when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tournamentService.generateBracket(tournamentId))
        .isInstanceOf(TournamentNotFoundException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  // --- deleteTournament ---
  @Test
  void deleteTournament_whenTournamentExists_deletesTournament() {
    Tournament tournament = registrationTournament();
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    tournamentService.deleteTournament(tournament.getId());
    verify(tournamentRepository).deleteById(tournament.getId());
  }

  @Test
  void deleteTournament_whenTournamentDoesNotExist_throwsNotFoundException() {
    UUID tournamentId = UUID.randomUUID();
    when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tournamentService.deleteTournament(tournamentId))
        .isInstanceOf(TournamentNotFoundException.class);
    verify(tournamentRepository, never()).deleteById(any(UUID.class));
  }

  // --- joinTournament ---

  @Test
  void joinTournament_whenRegistrationOpen_addsParticipantToTournament() {
    User player = player();
    Tournament tournament = registrationTournament();
    when(tournamentRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

    TournamentParticipant participant = tournamentService.joinTournament("ABC123", player);

    assertThat(participant.getUser()).isEqualTo(player);
    assertThat(participant.getTournament()).isEqualTo(tournament);
    assertThat(tournament.getParticipants()).containsExactly(participant);
    verify(tournamentRepository, times(1)).save(tournament);
  }

  @Test
  void joinTournament_whenNotRegistration_throwsException() {
    Tournament tournament = registrationTournament();
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    when(tournamentRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.joinTournament("ABC123", player()))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  @Test
  void joinTournament_whenBracketReady_throwsException() {
    Tournament tournament = registrationTournament();
    tournament.setStatus(TournamentStatus.BRACKET_READY);
    when(tournamentRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.joinTournament("ABC123", player()))
        .isInstanceOf(InvalidTournamentStateException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  void joinTournament_whenCompleted_throwsException() {
    Tournament tournament = registrationTournament();
    tournament.setStatus(TournamentStatus.COMPLETED);
    when(tournamentRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.joinTournament("ABC123", player()))
        .isInstanceOf(InvalidTournamentStateException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  void joinTournament_whenUserAlreadyJoined_throwsConflictException() {
    User existingUser = player();
    Tournament tournament = registrationTournament();
    TournamentParticipant existingParticipant =
        TournamentParticipant.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .user(existingUser)
            .build();
    tournament.getParticipants().add(existingParticipant);
    when(tournamentRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.joinTournament("ABC123", existingUser))
        .isInstanceOf(DuplicateTournamentParticipantException.class);
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  void joinTournament_whenJoinCodeNotFound_throwsNotFoundException() {
    when(tournamentRepository.findByJoinCode("BADCODE")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tournamentService.joinTournament("BADCODE", player()))
        .isInstanceOf(TournamentNotFoundException.class);
  }

  // --- createTournament ---

  @Test
  void createTournament_initializeTournamentWithCorrectDefaults() {
    User organizer = organizer();
    when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));
    Tournament result = tournamentService.createTournament(organizer, "Test Tournament");
    assertThat(result.getStatus()).isEqualTo(TournamentStatus.REGISTRATION);
    assertThat(result.getJoinCode()).matches("[A-Z0-9]{6}");
    assertThat(result.getOrganizer()).isEqualTo(organizer);
    assertThat(result.getTitle()).isEqualTo("Test Tournament");
  }

  @Test
  void createTournament_whenJoinCodeCollisionRetriesAndEventuallySucceeds() {
    User organizer = organizer();
    when(tournamentRepository.save(any(Tournament.class)))
        .thenThrow(
            new DataIntegrityViolationException("duplicate key value violates unique constraint"))
        .thenAnswer(inv -> inv.getArgument(0));

    Tournament result = tournamentService.createTournament(organizer, "Test Tournament");

    assertThat(result.getJoinCode()).matches("[A-Z0-9]{6}");
    verify(tournamentRepository, times(2)).save(any(Tournament.class));
  }

  @Test
  void createTournament_whenJoinCodeCollisionPersists_throwsJoinCodeGenerationException() {
    User organizer = organizer();
    when(tournamentRepository.save(any(Tournament.class)))
        .thenThrow(
            new DataIntegrityViolationException("duplicate key value violates unique constraint"));

    assertThatThrownBy(() -> tournamentService.createTournament(organizer, "Test Tournament"))
        .isInstanceOf(JoinCodeGenerationException.class);
    verify(tournamentRepository, times(5)).save(any(Tournament.class));
  }
}
