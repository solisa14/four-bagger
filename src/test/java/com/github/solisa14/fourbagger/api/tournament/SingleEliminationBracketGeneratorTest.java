package com.github.solisa14.fourbagger.api.tournament;

import static com.github.solisa14.fourbagger.api.testsupport.TestDataFactory.seededTeams;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SingleEliminationBracketGeneratorTest {

  private final SingleEliminationBracketGenerator generator =
      new SingleEliminationBracketGenerator();

  @Test
  void planBracket_whenFourTeams_createsWiredSingleEliminationTree() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 4);
    tournament.getTeams().addAll(teams);

    generator.planBracket(tournament, teams);

    assertThat(tournament.getRounds()).hasSize(2);
    assertThat(tournament.getRounds())
        .extracting(TournamentRound::getBracketType)
        .containsOnly(BracketType.WINNERS);
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
              assertThat(match.getWinnerNextMatch()).isEqualTo(roundTwo.getMatches().getFirst());
              assertThat(match.getLoserNextMatch()).isNull();
              assertThat(match.getLoserNextMatchPosition()).isNull();
            });
    assertThat(roundOne.getMatches())
        .extracting(Match::getWinnerNextMatchPosition)
        .containsExactlyInAnyOrder(1, 2);
  }

  @Test
  void planBracket_whenThreeTeams_marksByeAndAutoAdvancesWinner() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 3);
    tournament.getTeams().addAll(teams);

    generator.planBracket(tournament, teams);

    assertThat(tournament.getRounds()).hasSize(2);
    assertThat(tournament.getRounds())
        .extracting(TournamentRound::getBracketType)
        .containsOnly(BracketType.WINNERS);
    TournamentRound roundOne = round(tournament, 1);
    TournamentRound roundTwo = round(tournament, 2);
    assertThat(roundOne.getMatches()).hasSize(2);
    assertThat(roundTwo.getMatches()).hasSize(1);
    assertThat(roundOne.getMatches()).filteredOn(Match::isBye).hasSize(1);

    Match byeMatch = roundOne.getMatches().stream().filter(Match::isBye).findFirst().orElseThrow();
    assertThat(byeMatch.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(byeMatch.getWinner()).isNotNull();
    assertThat(byeMatch.getWinner().getSeed()).isEqualTo(1);
    assertThat(roundTwo.getMatches().getFirst().getTeamOne()).isEqualTo(byeMatch.getWinner());
  }

  @Test
  void planBracket_whenSevenTeams_createsEightSlotBracketWithOneByeForTopSeed() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 7);
    tournament.getTeams().addAll(teams);

    generator.planBracket(tournament, teams);

    List<TournamentRound> winners = winnersRounds(tournament);
    assertThat(winners).hasSize(3);
    assertThat(winners)
        .extracting(round -> round.getMatches().size())
        .containsExactly(4, 2, 1);

    List<Match> firstRound = winners.getFirst().getMatches();
    assertThat(firstRound).filteredOn(Match::isBye).hasSize(1);
    Match byeMatch = firstRound.stream().filter(Match::isBye).findFirst().orElseThrow();
    assertThat(byeMatch.getWinner().getSeed()).isEqualTo(1);
    assertThat(byeMatch.getStatus()).isEqualTo(MatchStatus.COMPLETED);

    Match winnerFinal = winners.getLast().getMatches().getFirst();
    for (int matchIndex = 0; matchIndex < firstRound.size(); matchIndex++) {
      assertRoute(
          firstRound.get(matchIndex),
          winners.get(1).getMatches().get(matchIndex / 2),
          (matchIndex % 2) + 1);
    }
    List<Match> secondRound = winners.get(1).getMatches();
    for (int matchIndex = 0; matchIndex < secondRound.size(); matchIndex++) {
      assertRoute(secondRound.get(matchIndex), winnerFinal, (matchIndex % 2) + 1);
    }
    assertThat(firstRound.getFirst().getTeamOne().getSeed()).isEqualTo(1);
  }

  @Test
  void planBracket_whenEightTeams_createsWiredWinnersBracket() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 8);
    tournament.getTeams().addAll(teams);

    generator.planBracket(tournament, teams);

    List<TournamentRound> winners = winnersRounds(tournament);
    assertThat(winners).hasSize(3);
    assertThat(winners)
        .extracting(round -> round.getMatches().size())
        .containsExactly(4, 2, 1);
    assertThat(winners.getFirst().getMatches()).filteredOn(Match::isBye).isEmpty();

    List<Match> firstRound = winners.getFirst().getMatches();
    Match winnerFinal = winners.getLast().getMatches().getFirst();
    for (int matchIndex = 0; matchIndex < firstRound.size(); matchIndex++) {
      assertRoute(
          firstRound.get(matchIndex),
          winners.get(1).getMatches().get(matchIndex / 2),
          (matchIndex % 2) + 1);
    }
    List<Match> secondRound = winners.get(1).getMatches();
    for (int matchIndex = 0; matchIndex < secondRound.size(); matchIndex++) {
      assertRoute(secondRound.get(matchIndex), winnerFinal, matchIndex + 1);
    }
    assertThat(firstRound.getFirst().getTeamOne().getSeed()).isEqualTo(1);
    assertThat(firstRound.getFirst().getTeamTwo().getSeed()).isEqualTo(8);
  }

  @Test
  void planBracket_whenSixteenTeams_createsWiredWinnersBracket() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 16);
    tournament.getTeams().addAll(teams);

    generator.planBracket(tournament, teams);

    List<TournamentRound> winners = winnersRounds(tournament);
    assertThat(winners).hasSize(4);
    assertThat(winners)
        .extracting(round -> round.getMatches().size())
        .containsExactly(8, 4, 2, 1);
    assertThat(winners.getFirst().getMatches()).filteredOn(Match::isBye).isEmpty();

    List<Match> firstRound = winners.getFirst().getMatches();
    for (int matchIndex = 0; matchIndex < firstRound.size(); matchIndex++) {
      assertRoute(
          firstRound.get(matchIndex),
          winners.get(1).getMatches().get(matchIndex / 2),
          (matchIndex % 2) + 1);
    }
    List<Match> secondRound = winners.get(1).getMatches();
    for (int matchIndex = 0; matchIndex < secondRound.size(); matchIndex++) {
      assertRoute(
          secondRound.get(matchIndex),
          winners.get(2).getMatches().get(matchIndex / 2),
          (matchIndex % 2) + 1);
    }
    List<Match> thirdRound = winners.get(2).getMatches();
    Match winnerFinal = winners.getLast().getMatches().getFirst();
    for (int matchIndex = 0; matchIndex < thirdRound.size(); matchIndex++) {
      assertRoute(thirdRound.get(matchIndex), winnerFinal, matchIndex + 1);
    }
  }

  @Test
  void planBracket_whenSixTeams_assignsTwoByesToTopSeeds() {
    Tournament tournament = tournament();
    List<TournamentTeam> teams = seededTeams(tournament, 6);
    tournament.getTeams().addAll(teams);

    generator.planBracket(tournament, teams);

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

    generator.planBracket(tournament, teams);

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
            .bracketType(BracketType.WINNERS)
            .roundNumber(1)
            .bestOf(3)
            .scoringMode(ScoringMode.EXACT)
            .build();
    TournamentRound roundTwo =
        TournamentRound.builder()
            .tournament(tournament)
            .bracketType(BracketType.WINNERS)
            .roundNumber(2)
            .bestOf(5)
            .scoringMode(ScoringMode.STANDARD)
            .build();
    tournament.getRounds().add(roundOne);
    tournament.getRounds().add(roundTwo);
    List<TournamentTeam> teams = seededTeams(tournament, 4);
    tournament.getTeams().addAll(teams);

    generator.planBracket(tournament, teams);

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

  private User user(String suffix) {
    return TestDataFactory.user(
        UUID.randomUUID(), suffix, "encoded", Role.USER);
  }

  private List<TournamentRound> winnersRounds(Tournament tournament) {
    return tournament.getRounds().stream()
        .filter(round -> round.getBracketType() == BracketType.WINNERS)
        .sorted(java.util.Comparator.comparing(TournamentRound::getRoundNumber))
        .toList();
  }

  private TournamentRound round(Tournament tournament, int roundNumber) {
    return tournament.getRounds().stream()
        .filter(
            round ->
                round.getBracketType() == BracketType.WINNERS
                    && round.getRoundNumber() == roundNumber)
        .findFirst()
        .orElseThrow();
  }

  private void assertRoute(Match source, Match destination, int position) {
    assertThat(source.getWinnerNextMatch()).isSameAs(destination);
    assertThat(source.getWinnerNextMatchPosition()).isEqualTo(position);
    assertThat(source.getLoserNextMatch()).isNull();
    assertThat(source.getLoserNextMatchPosition()).isNull();
  }
}
