package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TournamentBracketServiceTest {

  private final TournamentBracketService tournamentBracketService = new TournamentBracketService();

  @Test
  void planBracket_whenFourTeams_createsWiredSingleEliminationTree() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 4);
    tournament.getTeams().addAll(teams);

    tournamentBracketService.planBracket(tournament, teams);

    assertThat(tournament.getRounds()).hasSize(2);
    TournamentRound roundOne =
        tournament.getRounds().stream()
            .filter(r -> r.getRoundNumber() == 1)
            .findFirst()
            .orElseThrow();
    TournamentRound roundTwo =
        tournament.getRounds().stream()
            .filter(r -> r.getRoundNumber() == 2)
            .findFirst()
            .orElseThrow();
    assertThat(roundOne.getMatches()).hasSize(2);
    assertThat(roundTwo.getMatches()).hasSize(1);
    assertThat(roundOne.getMatches()).allSatisfy(match -> assertThat(match.isBye()).isFalse());
    assertThat(roundOne.getMatches())
        .allSatisfy(
            match -> {
              assertThat(match.getNextMatch()).isEqualTo(roundTwo.getMatches().getFirst());
              assertThat(match.getNextMatchPosition()).isIn(1, 2);
            });
  }

  @Test
  void planBracket_whenThreeTeams_marksByeAndAutoAdvancesWinner() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 3);
    tournament.getTeams().addAll(teams);

    tournamentBracketService.planBracket(tournament, teams);

    TournamentRound roundOne =
        tournament.getRounds().stream()
            .filter(r -> r.getRoundNumber() == 1)
            .findFirst()
            .orElseThrow();
    TournamentRound roundTwo =
        tournament.getRounds().stream()
            .filter(r -> r.getRoundNumber() == 2)
            .findFirst()
            .orElseThrow();
    Match byeMatch = roundOne.getMatches().stream().filter(Match::isBye).findFirst().orElseThrow();

    assertThat(byeMatch.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(byeMatch.getWinner()).isNotNull();
    assertThat(roundTwo.getMatches().getFirst().getTeamOne()).isEqualTo(byeMatch.getWinner());
  }

  @Test
  void planBracket_whenSixTeams_assignsTwoByesToTopSeeds() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 6);
    tournament.getTeams().addAll(teams);

    tournamentBracketService.planBracket(tournament, teams);

    TournamentRound roundOne =
        tournament.getRounds().stream()
            .filter(r -> r.getRoundNumber() == 1)
            .findFirst()
            .orElseThrow();

    assertThat(roundOne.getMatches()).hasSize(4);
    assertThat(roundOne.getMatches()).filteredOn(Match::isBye).hasSize(2);
    assertThat(
            roundOne.getMatches().stream()
                .filter(Match::isBye)
                .map(m -> m.getWinner().getSeed())
                .toList())
        .containsExactlyInAnyOrder(1, 2);
  }

  @Test
  void planBracket_whenFiveTeams_assignsThreeByesToTopSeeds() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 5);
    tournament.getTeams().addAll(teams);

    tournamentBracketService.planBracket(tournament, teams);

    TournamentRound roundOne =
        tournament.getRounds().stream()
            .filter(r -> r.getRoundNumber() == 1)
            .findFirst()
            .orElseThrow();

    assertThat(roundOne.getMatches()).hasSize(4);
    assertThat(roundOne.getMatches()).filteredOn(Match::isBye).hasSize(3);
    assertThat(
            roundOne.getMatches().stream()
                .filter(Match::isBye)
                .map(m -> m.getWinner().getSeed())
                .toList())
        .containsExactlyInAnyOrder(1, 2, 3);
  }

  @Test
  void planBracket_whenRoundsAlreadyConfigured_preservesRoundRules() {
    Tournament tournament = tournament();
    TournamentRound roundOne =
        TournamentRound.builder()
            .tournament(tournament)
            .roundNumber(1)
            .bestOf(3)
            .scoringMode(ScoringMode.EXACT)
            .build();
    TournamentRound roundTwo =
        TournamentRound.builder()
            .tournament(tournament)
            .roundNumber(2)
            .bestOf(5)
            .scoringMode(ScoringMode.STANDARD)
            .build();
    tournament.getRounds().add(roundOne);
    tournament.getRounds().add(roundTwo);
    List<TournamentTeam> teams = seededTeams(tournament, 4);
    tournament.getTeams().addAll(teams);

    tournamentBracketService.planBracket(tournament, teams);

    assertThat(roundOne.getBestOf()).isEqualTo(3);
    assertThat(roundOne.getScoringMode()).isEqualTo(ScoringMode.EXACT);
    assertThat(roundTwo.getBestOf()).isEqualTo(5);
    assertThat(roundTwo.getScoringMode()).isEqualTo(ScoringMode.STANDARD);
    assertThat(roundOne.getMatches()).hasSize(2);
    assertThat(roundTwo.getMatches()).hasSize(1);
  }

  private Tournament tournament() {
    return Tournament.builder()
        .id(UUID.randomUUID())
        .organizer(user("organizer"))
        .title("Test")
        .status(TournamentStatus.BRACKET_READY)
        .joinCode("ABC123")
        .build();
  }

  private List<TournamentTeam> seededTeams(Tournament tournament, int count) {
    return java.util.stream.IntStream.rangeClosed(1, count)
        .mapToObj(
            seed ->
                TournamentTeam.builder()
                    .id(UUID.randomUUID())
                    .tournament(tournament)
                    .playerOne(user("p" + seed))
                    .seed(seed)
                    .build())
        .toList();
  }

  private User user(String suffix) {
    return TestDataFactory.user(
        UUID.randomUUID(), suffix, suffix + "@example.com", "encoded", Role.USER);
  }
}
