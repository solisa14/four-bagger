package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.testsupport.AbstractDataJpaTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class TournamentParticipationModePersistenceTest extends AbstractDataJpaTest {

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void persistsSelfJoinTournamentWithJoinCodeAndMode() {
    User organizer = savedUser("self");
    Tournament saved =
        tournamentRepository.saveAndFlush(
            Tournament.builder()
                .organizer(organizer)
                .title("Self Join Cup")
                .status(TournamentStatus.REGISTRATION)
                .gameType(GameType.SINGLES)
                .format(TournamentFormat.SINGLE_ELIMINATION)
                .participationMode(TournamentParticipationMode.SELF_JOIN)
                .joinCode("SELF01")
                .build());

    entityManager.clear();
    Tournament reloaded = tournamentRepository.findById(saved.getId()).orElseThrow();

    assertThat(reloaded.getParticipationMode()).isEqualTo(TournamentParticipationMode.SELF_JOIN);
    assertThat(reloaded.getJoinCode()).isEqualTo("SELF01");
  }

  @Test
  void persistsOrganizerManagedTournamentWithoutJoinCode() {
    User organizer = savedUser("managed");
    Tournament saved =
        tournamentRepository.saveAndFlush(
            Tournament.builder()
                .organizer(organizer)
                .title("Managed Cup")
                .status(TournamentStatus.REGISTRATION)
                .gameType(GameType.SINGLES)
                .format(TournamentFormat.SINGLE_ELIMINATION)
                .participationMode(TournamentParticipationMode.ORGANIZER_MANAGED)
                .joinCode(null)
                .build());

    entityManager.clear();
    Tournament reloaded = tournamentRepository.findById(saved.getId()).orElseThrow();

    assertThat(reloaded.getParticipationMode())
        .isEqualTo(TournamentParticipationMode.ORGANIZER_MANAGED);
    assertThat(reloaded.getJoinCode()).isNull();
  }

  @Test
  void persistsGuestParticipantWithoutUserAndEnforcesNormalizedUniqueDisplayName() {
    User organizer = savedUser("guest-org");
    Tournament tournament =
        Tournament.builder()
            .organizer(organizer)
            .title("Guest Cup")
            .status(TournamentStatus.REGISTRATION)
            .gameType(GameType.SINGLES)
            .format(TournamentFormat.SINGLE_ELIMINATION)
            .participationMode(TournamentParticipationMode.ORGANIZER_MANAGED)
            .build();
    tournament.addGuestParticipant("Pat Riley");
    UUID tournamentId = tournamentRepository.saveAndFlush(tournament).getId();
    entityManager.clear();

    Tournament reloaded = tournamentRepository.findDetailById(tournamentId).orElseThrow();
    assertThat(reloaded.getParticipants()).hasSize(1);
    TournamentParticipant guest = reloaded.getParticipants().getFirst();
    assertThat(guest.getUser()).isNull();
    assertThat(guest.getDisplayName()).isEqualTo("Pat Riley");

    // Bypass domain validation to assert the DB unique index on normalized names.
    reloaded
        .getParticipants()
        .add(
            TournamentParticipant.builder()
                .tournament(reloaded)
                .displayName("pat riley")
                .build());
    assertThatThrownBy(() -> tournamentRepository.saveAndFlush(reloaded))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void persistsAccountParticipantWithoutDisplayName() {
    User organizer = savedUser("acct-org");
    User player = savedUser("acct-player");
    Tournament tournament =
        TestDataFactory.tournament(null, organizer, "Account Cup", "ACCT01");
    tournament
        .getParticipants()
        .add(TournamentParticipant.builder().tournament(tournament).user(player).build());

    UUID tournamentId = tournamentRepository.saveAndFlush(tournament).getId();
    entityManager.clear();

    Tournament reloaded = tournamentRepository.findDetailById(tournamentId).orElseThrow();
    TournamentParticipant participant = reloaded.getParticipants().getFirst();
    assertThat(participant.getUser().getUsername()).isEqualTo(player.getUsername());
    assertThat(participant.getDisplayName()).isNull();
    assertThat(participant.isGuest()).isFalse();
  }

  private User savedUser(String suffix) {
    return userRepository.saveAndFlush(
        TestDataFactory.user(null, "user" + suffix, "encoded", Role.USER));
  }
}
