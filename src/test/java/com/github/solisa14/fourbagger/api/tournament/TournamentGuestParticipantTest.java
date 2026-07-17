package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import org.junit.jupiter.api.Test;

class TournamentGuestParticipantTest {

  @Test
  void addGuestParticipant_trimsWhitespaceAndPreservesCasing() {
    Tournament tournament = organizerManagedTournament();

    TournamentParticipant guest = tournament.addGuestParticipant("  Pat Riley  ");

    assertThat(guest.getDisplayName()).isEqualTo("Pat Riley");
    assertThat(guest.getUser()).isNull();
    assertThat(guest.isGuest()).isTrue();
    assertThat(tournament.getParticipants()).containsExactly(guest);
  }

  @Test
  void addGuestParticipant_rejectsDuplicateNormalizedNames() {
    Tournament tournament = organizerManagedTournament();
    tournament.addGuestParticipant("Alex");

    assertThatThrownBy(() -> tournament.addGuestParticipant("  alex "))
        .isInstanceOf(DuplicateGuestDisplayNameException.class);
    assertThat(tournament.getParticipants()).hasSize(1);
  }

  @Test
  void addGuestParticipant_rejectsBlankName() {
    Tournament tournament = organizerManagedTournament();

    assertThatThrownBy(() -> tournament.addGuestParticipant("   "))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("display name");
  }

  @Test
  void addGuestParticipant_rejectsSelfJoinTournaments() {
    Tournament tournament =
        TestDataFactory.tournament(
            null,
            TestDataFactory.user(null, "org", "encoded", Role.USER),
            "Self Join Cup",
            "ABC123");

    assertThatThrownBy(() -> tournament.addGuestParticipant("Alex"))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("organizer-managed");
  }

  @Test
  void updateGuestDisplayName_trimsWhitespaceAndPreservesCasing() {
    Tournament tournament = organizerManagedTournament();
    TournamentParticipant guest = tournament.addGuestParticipant("Alex");
    guest.setId(java.util.UUID.randomUUID());

    tournament.updateGuestDisplayName(guest.getId(), "  Pat Riley  ");

    assertThat(guest.getDisplayName()).isEqualTo("Pat Riley");
  }

  @Test
  void updateGuestDisplayName_rejectsDuplicateNormalizedNames() {
    Tournament tournament = organizerManagedTournament();
    TournamentParticipant guest = tournament.addGuestParticipant("Alex");
    guest.setId(java.util.UUID.randomUUID());
    TournamentParticipant other = tournament.addGuestParticipant("Pat");
    other.setId(java.util.UUID.randomUUID());

    assertThatThrownBy(() -> tournament.updateGuestDisplayName(guest.getId(), "  pat "))
        .isInstanceOf(DuplicateGuestDisplayNameException.class);
    assertThat(guest.getDisplayName()).isEqualTo("Alex");
  }

  @Test
  void updateGuestDisplayName_allowsSameNormalizedNameForSameGuest() {
    Tournament tournament = organizerManagedTournament();
    TournamentParticipant guest = tournament.addGuestParticipant("Alex");
    guest.setId(java.util.UUID.randomUUID());

    tournament.updateGuestDisplayName(guest.getId(), "  alex ");

    assertThat(guest.getDisplayName()).isEqualTo("alex");
  }

  @Test
  void updateGuestDisplayName_rejectsBlankName() {
    Tournament tournament = organizerManagedTournament();
    TournamentParticipant guest = tournament.addGuestParticipant("Alex");
    guest.setId(java.util.UUID.randomUUID());

    assertThatThrownBy(() -> tournament.updateGuestDisplayName(guest.getId(), "   "))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("display name");
  }

  @Test
  void updateGuestDisplayName_rejectsUnknownParticipant() {
    Tournament tournament = organizerManagedTournament();

    assertThatThrownBy(
            () -> tournament.updateGuestDisplayName(java.util.UUID.randomUUID(), "Alex"))
        .isInstanceOf(TournamentParticipantNotFoundException.class);
  }

  @Test
  void updateGuestDisplayName_rejectsAccountParticipant() {
    Tournament tournament = organizerManagedTournament();
    TournamentParticipant accountParticipant =
        TournamentParticipant.builder()
            .id(java.util.UUID.randomUUID())
            .tournament(tournament)
            .user(TestDataFactory.user(java.util.UUID.randomUUID(), "player", "encoded", Role.USER))
            .build();
    tournament.getParticipants().add(accountParticipant);

    assertThatThrownBy(
            () -> tournament.updateGuestDisplayName(accountParticipant.getId(), "Alex"))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("guest");
  }

  @Test
  void updateGuestDisplayName_rejectsSelfJoinTournaments() {
    Tournament tournament =
        TestDataFactory.tournament(
            null,
            TestDataFactory.user(null, "org", "encoded", Role.USER),
            "Self Join Cup",
            "ABC123");
    TournamentParticipant guest =
        TournamentParticipant.builder()
            .id(java.util.UUID.randomUUID())
            .tournament(tournament)
            .displayName("Alex")
            .build();
    tournament.getParticipants().add(guest);

    assertThatThrownBy(() -> tournament.updateGuestDisplayName(guest.getId(), "Pat"))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("organizer-managed");
  }

  private Tournament organizerManagedTournament() {
    return Tournament.builder()
        .organizer(TestDataFactory.user(null, "org", "encoded", Role.USER))
        .title("Managed Cup")
        .status(TournamentStatus.REGISTRATION)
        .gameType(GameType.SINGLES)
        .format(TournamentFormat.SINGLE_ELIMINATION)
        .participationMode(TournamentParticipationMode.ORGANIZER_MANAGED)
        .build();
  }
}
