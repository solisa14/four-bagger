package com.github.solisa14.fourbagger.api.tournament;

import static com.github.solisa14.fourbagger.api.testsupport.TestDataFactory.seededTeams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class DoubleEliminationBracketGeneratorTest {

  private final DoubleEliminationBracketGenerator generator =
      new DoubleEliminationBracketGenerator();

  @Test
  void format_returnsDoubleElimination() {
    assertThat(generator.format()).isEqualTo(TournamentFormat.DOUBLE_ELIMINATION);
  }

  @Test
  void planBracket_whenFourTeams_createsWiredDoubleEliminationTree() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = addTeams(tournament, 4);

    generator.planBracket(tournament, teams);

    assertThat(tournament.getRounds()).hasSize(5);
    TournamentRound winnerRoundOne = round(tournament, BracketType.WINNERS, 1);
    TournamentRound winnerRoundTwo = round(tournament, BracketType.WINNERS, 2);
    TournamentRound loserRoundOne = round(tournament, BracketType.LOSERS, 1);
    TournamentRound loserRoundTwo = round(tournament, BracketType.LOSERS, 2);
    TournamentRound finalRound = round(tournament, BracketType.FINAL, 1);

    assertThat(winnerRoundOne.getMatches()).hasSize(2);
    assertThat(winnerRoundTwo.getMatches()).hasSize(1);
    assertThat(loserRoundOne.getMatches()).hasSize(1);
    assertThat(loserRoundTwo.getMatches()).hasSize(1);
    assertThat(finalRound.getMatches()).hasSize(1);
    assertThat(tournament.getRounds())
        .extracting(TournamentRound::getBracketType)
        .doesNotContain(BracketType.GRAND_FINAL);

    Match winnerFinal = winnerRoundTwo.getMatches().getFirst();
    Match loserRoundOneMatch = loserRoundOne.getMatches().getFirst();
    Match loserFinal = loserRoundTwo.getMatches().getFirst();
    Match championshipMatch = finalRound.getMatches().getFirst();

    assertThat(winnerRoundOne.getMatches())
        .allSatisfy(
            match -> {
              assertThat(match.getWinnerNextMatch()).isSameAs(winnerFinal);
              assertThat(match.getLoserNextMatch()).isSameAs(loserRoundOneMatch);
            });
    assertThat(winnerRoundOne.getMatches())
        .extracting(Match::getWinnerNextMatchPosition)
        .containsExactly(1, 2);
    assertThat(winnerRoundOne.getMatches())
        .extracting(Match::getLoserNextMatchPosition)
        .containsExactly(1, 2);

    assertRoute(loserRoundOneMatch, loserFinal, 1);
    assertThat(loserRoundOneMatch.getLoserNextMatch()).isNull();
    assertThat(loserRoundOneMatch.getLoserNextMatchPosition()).isNull();

    assertRoute(winnerFinal, championshipMatch, 1);
    assertThat(winnerFinal.getLoserNextMatch()).isSameAs(loserFinal);
    assertThat(winnerFinal.getLoserNextMatchPosition()).isEqualTo(2);

    assertRoute(loserFinal, championshipMatch, 2);
    assertThat(loserFinal.getLoserNextMatch()).isNull();
    assertThat(loserFinal.getLoserNextMatchPosition()).isNull();

    assertThat(championshipMatch.getWinnerNextMatch()).isNull();
    assertThat(championshipMatch.getWinnerNextMatchPosition()).isNull();
    assertThat(championshipMatch.getLoserNextMatch()).isNull();
    assertThat(championshipMatch.getLoserNextMatchPosition()).isNull();

    assertThat(winnerRoundOne.getMatches().getFirst().getTeamOne().getSeed()).isEqualTo(1);
    assertThat(winnerRoundOne.getMatches().getFirst().getTeamTwo().getSeed()).isEqualTo(4);
    assertThat(winnerRoundOne.getMatches().getLast().getTeamOne().getSeed()).isEqualTo(2);
    assertThat(winnerRoundOne.getMatches().getLast().getTeamTwo().getSeed()).isEqualTo(3);
  }

  @Test
  void planBracket_whenEightTeams_createsAllRoundsAndCrossoverRoutes() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = addTeams(tournament, 8);

    generator.planBracket(tournament, teams);

    List<TournamentRound> winners = rounds(tournament, BracketType.WINNERS);
    List<TournamentRound> losers = rounds(tournament, BracketType.LOSERS);
    Match championship = round(tournament, BracketType.FINAL, 1).getMatches().getFirst();

    assertThat(winners).hasSize(3);
    assertThat(winners)
        .extracting(round -> round.getMatches().size())
        .containsExactly(4, 2, 1);
    assertThat(losers).hasSize(4);
    assertThat(losers)
        .extracting(round -> round.getMatches().size())
        .containsExactly(2, 2, 1, 1);

    List<Match> winnerRoundOne = winners.getFirst().getMatches();
    for (int matchIndex = 0; matchIndex < winnerRoundOne.size(); matchIndex++) {
      assertRoute(
          winnerRoundOne.get(matchIndex),
          winners.get(1).getMatches().get(matchIndex / 2),
          (matchIndex % 2) + 1);
      assertLoserRoute(
          winnerRoundOne.get(matchIndex),
          losers.getFirst().getMatches().get(matchIndex / 2),
          (matchIndex % 2) + 1);
    }

    List<Match> winnerRoundTwo = winners.get(1).getMatches();
    for (int matchIndex = 0; matchIndex < winnerRoundTwo.size(); matchIndex++) {
      assertRoute(
          winnerRoundTwo.get(matchIndex),
          winners.getLast().getMatches().getFirst(),
          matchIndex + 1);
      assertLoserRoute(
          winnerRoundTwo.get(matchIndex),
          losers.get(1).getMatches().get(1 - matchIndex),
          2);
    }

    Match winnerFinal = winners.getLast().getMatches().getFirst();
    assertRoute(winnerFinal, championship, 1);
    assertLoserRoute(winnerFinal, losers.getLast().getMatches().getFirst(), 2);

    assertRoute(
        losers.getFirst().getMatches().getFirst(),
        losers.get(1).getMatches().getFirst(),
        1);
    assertRoute(
        losers.getFirst().getMatches().getLast(),
        losers.get(1).getMatches().getLast(),
        1);
    assertRoute(
        losers.get(1).getMatches().getFirst(),
        losers.get(2).getMatches().getFirst(),
        1);
    assertRoute(
        losers.get(1).getMatches().getLast(),
        losers.get(2).getMatches().getFirst(),
        2);
    assertRoute(
        losers.get(2).getMatches().getFirst(),
        losers.getLast().getMatches().getFirst(),
        1);
    assertRoute(losers.getLast().getMatches().getFirst(), championship, 2);
    assertThat(losers)
        .flatExtracting(TournamentRound::getMatches)
        .allSatisfy(
            match -> {
              assertThat(match.getLoserNextMatch()).isNull();
              assertThat(match.getLoserNextMatchPosition()).isNull();
            });
  }

  @Test
  void planBracket_whenFiveTeams_assignsByesToTopSeedsAndResolvesEmptyLosersPath() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = addTeams(tournament, 5);

    generator.planBracket(tournament, teams);

    List<Match> firstWinnerRound = round(tournament, BracketType.WINNERS, 1).getMatches();
    List<Match> byeMatches = firstWinnerRound.stream().filter(Match::isBye).toList();
    assertThat(byeMatches).hasSize(3);
    assertThat(byeMatches)
        .extracting(match -> match.getWinner().getSeed())
        .containsExactly(1, 2, 3);
    assertThat(byeMatches)
        .allSatisfy(
            match -> {
              assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
              assertThat(match.getTeamTwo()).isNull();
            });

    TournamentRound winnerRoundTwo = round(tournament, BracketType.WINNERS, 2);
    assertThat(winnerRoundTwo.getMatches().getFirst().getTeamOne().getSeed()).isEqualTo(1);
    assertThat(winnerRoundTwo.getMatches().getFirst().getTeamTwo().getSeed()).isEqualTo(2);
    assertThat(winnerRoundTwo.getMatches().getLast().getTeamOne().getSeed()).isEqualTo(3);
    assertThat(winnerRoundTwo.getMatches().getLast().getTeamTwo()).isNull();

    Match emptyLosersMatch = round(tournament, BracketType.LOSERS, 1).getMatches().getFirst();
    assertThat(emptyLosersMatch.isBye()).isTrue();
    assertThat(emptyLosersMatch.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(emptyLosersMatch.getWinner()).isNull();
  }

  @Test
  void planBracket_whenRoundsAlreadyConfigured_preservesRulesAndRemovesUnusedRounds() {
    Tournament tournament = tournament();
    TournamentRound winnerRound = configuredRound(tournament, BracketType.WINNERS, 1, 3);
    TournamentRound loserRound = configuredRound(tournament, BracketType.LOSERS, 1, 5);
    TournamentRound finalRound = configuredRound(tournament, BracketType.FINAL, 1, 7);
    TournamentRound extraWinnerRound =
        configuredRound(tournament, BracketType.WINNERS, 3, 1);
    TournamentRound obsoleteGrandFinal =
        configuredRound(tournament, BracketType.GRAND_FINAL, 1, 1);
    tournament
        .getRounds()
        .addAll(
            List.of(
                winnerRound,
                loserRound,
                finalRound,
                extraWinnerRound,
                obsoleteGrandFinal));
    List<TournamentTeam> teams = addTeams(tournament, 4);

    generator.planBracket(tournament, teams);

    assertThat(tournament.getRounds()).contains(winnerRound, loserRound, finalRound);
    assertThat(tournament.getRounds()).doesNotContain(extraWinnerRound, obsoleteGrandFinal);
    assertThat(winnerRound.getBestOf()).isEqualTo(3);
    assertThat(winnerRound.getScoringMode()).isEqualTo(ScoringMode.EXACT);
    assertThat(loserRound.getBestOf()).isEqualTo(5);
    assertThat(loserRound.getScoringMode()).isEqualTo(ScoringMode.EXACT);
    assertThat(finalRound.getBestOf()).isEqualTo(7);
    assertThat(finalRound.getScoringMode()).isEqualTo(ScoringMode.EXACT);
    assertThat(tournament.getRounds()).hasSize(5);
  }

  @Test
  void planBracket_whenCalledAgain_replacesMatchesWithoutDuplicatingRounds() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = addTeams(tournament, 4);
    generator.planBracket(tournament, teams);
    List<Match> originalMatches =
        tournament.getRounds().stream().flatMap(round -> round.getMatches().stream()).toList();

    generator.planBracket(tournament, teams);

    List<Match> rebuiltMatches =
        tournament.getRounds().stream().flatMap(round -> round.getMatches().stream()).toList();
    assertThat(tournament.getRounds()).hasSize(5);
    assertThat(rebuiltMatches).hasSize(6);
    assertThat(rebuiltMatches).doesNotContainAnyElementsOf(originalMatches);
  }

  @Test
  void planBracket_resetsTeamLossAndEliminationState() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = addTeams(tournament, 4);
    teams.forEach(
        team -> {
          team.setLosses(2);
          team.setEliminated(true);
        });

    generator.planBracket(tournament, teams);

    assertThat(teams)
        .allSatisfy(
            team -> {
              assertThat(team.getLosses()).isZero();
              assertThat(team.isEliminated()).isFalse();
            });
  }

  @Test
  void planBracket_whenFewerThanFourTeams_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = addTeams(tournament, 3);

    assertThatThrownBy(() -> generator.planBracket(tournament, teams))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("at least 4 teams");
  }

  @Test
  void planBracket_whenSeedIsMissing_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = addTeams(tournament, 4);
    teams.getFirst().setSeed(null);

    assertInvalidSeed(tournament, teams);
  }

  @Test
  void planBracket_whenSeedIsDuplicated_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = addTeams(tournament, 4);
    teams.getLast().setSeed(teams.getFirst().getSeed());

    assertInvalidSeed(tournament, teams);
  }

  @Test
  void planBracket_whenSeedExceedsBracketSize_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = addTeams(tournament, 4);
    teams.getLast().setSeed(5);

    assertInvalidSeed(tournament, teams);
  }

  private Tournament tournament() {
    return Tournament.builder().format(TournamentFormat.DOUBLE_ELIMINATION).build();
  }

  private List<TournamentTeam> addTeams(Tournament tournament, int count) {
    List<TournamentTeam> teams = seededTeams(tournament, count);
    tournament.getTeams().addAll(teams);
    return teams;
  }

  private TournamentRound configuredRound(
      Tournament tournament, BracketType bracketType, int roundNumber, int bestOf) {
    return TournamentRound.builder()
        .tournament(tournament)
        .bracketType(bracketType)
        .roundNumber(roundNumber)
        .bestOf(bestOf)
        .scoringMode(ScoringMode.EXACT)
        .build();
  }

  private List<TournamentRound> rounds(Tournament tournament, BracketType bracketType) {
    return tournament.getRounds().stream()
        .filter(round -> round.getBracketType() == bracketType)
        .sorted(java.util.Comparator.comparing(TournamentRound::getRoundNumber))
        .toList();
  }

  private TournamentRound round(
      Tournament tournament, BracketType bracketType, int roundNumber) {
    return tournament.getRounds().stream()
        .filter(
            round ->
                round.getBracketType() == bracketType
                    && round.getRoundNumber() == roundNumber)
        .findFirst()
        .orElseThrow();
  }

  private void assertRoute(Match source, Match destination, int position) {
    assertThat(source.getWinnerNextMatch()).isSameAs(destination);
    assertThat(source.getWinnerNextMatchPosition()).isEqualTo(position);
  }

  private void assertLoserRoute(Match source, Match destination, int position) {
    assertThat(source.getLoserNextMatch()).isSameAs(destination);
    assertThat(source.getLoserNextMatchPosition()).isEqualTo(position);
  }

  private void assertInvalidSeed(Tournament tournament, List<TournamentTeam> teams) {
    assertThatThrownBy(() -> generator.planBracket(tournament, teams))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("Invalid seed assignment");
    assertThat(tournament.getRounds()).isEmpty();
  }
}
