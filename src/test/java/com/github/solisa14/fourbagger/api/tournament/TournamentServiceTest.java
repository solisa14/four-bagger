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

  // --- removeParticipant ---
  @Test
  void removeParticipant_whenTournamentIsInRegistration_removesParticipant() {
    Tournament tournament = registrationTournament();
    TournamentTeam team =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(player())
            .build();
    tournament.getTeams().add(team);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    tournamentService.removeParticipant(tournament.getId(), team.getId());
    assertThat(tournament.getTeams()).isEmpty();
    verify(tournamentRepository).save(tournament);
  }

  @Test
  void removeParticipant_whenTournamentHasMultipleTeams_removesOnlyTargetTeam() {
    Tournament tournament = registrationTournament();
    TournamentTeam teamOne =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(player())
            .build();
    TournamentTeam teamTwo =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(player())
            .build();
    tournament.getTeams().add(teamOne);
    tournament.getTeams().add(teamTwo);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));

    tournamentService.removeParticipant(tournament.getId(), teamOne.getId());

    assertThat(tournament.getTeams()).containsExactly(teamTwo);
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
  void removeParticipant_whenTeamDoesNotExist_throwsNotFoundException() {
    Tournament tournament = registrationTournament();
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    assertThatThrownBy(
            () -> tournamentService.removeParticipant(tournament.getId(), UUID.randomUUID()))
        .isInstanceOf(TournamentTeamNotFoundException.class);
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
  void joinTournament_whenRegistrationOpen_addsTeamToTournament() {
    User player = player();
    Tournament tournament = registrationTournament();
    when(tournamentRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

    TournamentTeam team = tournamentService.joinTournament("ABC123", player);

    assertThat(team.getPlayerOne()).isEqualTo(player);
    assertThat(team.getTournament()).isEqualTo(tournament);
    assertThat(tournament.getTeams()).containsExactly(team);
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
    TournamentTeam existingTeam =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(existingUser)
            .build();
    tournament.getTeams().add(existingTeam);
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
