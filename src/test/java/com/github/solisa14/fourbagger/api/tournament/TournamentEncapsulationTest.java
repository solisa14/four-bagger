package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TournamentEncapsulationTest {

  @Test
  void aggregateCollectionsRejectExternalMutationAndSynchronizeOwnership() {
    Tournament tournament = tournament("one");
    TournamentTeam team =
        TournamentTeam.builder().playerOne(user("player")).seed(1).build();
    TournamentRound round =
        TournamentRound.builder()
            .bracketType(BracketType.WINNERS)
            .roundNumber(1)
            .bestOf(1)
            .scoringMode(ScoringMode.STANDARD)
            .build();
    Match match = Match.builder().matchNumber(1).build();

    tournament.addTeam(team);
    tournament.addRound(round);
    round.addMatch(match);

    assertThat(team.getTournament()).isSameAs(tournament);
    assertThat(round.getTournament()).isSameAs(tournament);
    assertThat(match.getRound()).isSameAs(round);
    assertThatThrownBy(() -> tournament.getTeams().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> tournament.getRounds().add(round))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> round.getMatches().clear())
        .isInstanceOf(UnsupportedOperationException.class);

    tournament.removeRound(round);
    assertThat(round.getTournament()).isNull();
  }

  @Test
  void childCannotBeMovedToAnotherParent() {
    Tournament first = tournament("one");
    Tournament second = tournament("two");
    TournamentTeam team =
        TournamentTeam.builder().playerOne(user("player")).seed(1).build();
    first.addTeam(team);

    assertThatThrownBy(() -> second.addTeam(team))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("another tournament");
  }

  @Test
  void matchRouteRequiresAValidPairedPosition() {
    Match match = Match.builder().matchNumber(1).build();
    Match next = Match.builder().matchNumber(2).build();

    assertThatThrownBy(() -> match.configureWinnerRoute(next, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> match.configureWinnerRoute(next, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private Tournament tournament(String suffix) {
    return Tournament.restore(
        UUID.randomUUID(),
        user("organizer-" + suffix),
        "Tournament " + suffix,
        "CODE" + suffix,
        TournamentStatus.REGISTRATION,
        GameType.SINGLES);
  }

  private User user(String suffix) {
    return TestDataFactory.user(
        UUID.randomUUID(), suffix, suffix + "@example.com", "encoded", Role.USER);
  }
}
