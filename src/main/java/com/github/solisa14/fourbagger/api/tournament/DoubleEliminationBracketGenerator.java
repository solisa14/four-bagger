package com.github.solisa14.fourbagger.api.tournament;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/** Generates and seeds winners, losers, and championship rounds for double elimination. */
@Component
public class DoubleEliminationBracketGenerator implements TournamentBracketGenerator {

  private static final int MINIMUM_TEAM_COUNT = 4;

  @Override
  public TournamentFormat format() {
    return TournamentFormat.DOUBLE_ELIMINATION;
  }

  @Override
  public void planBracket(Tournament tournament, List<TournamentTeam> seededTeams) {
    if (seededTeams.size() < MINIMUM_TEAM_COUNT) {
      throw new InvalidTournamentStateException(
          "Double-elimination tournaments require at least 4 teams");
    }

    int bracketSize = calculateBracketSize(seededTeams.size());
    validateSeeds(seededTeams, bracketSize);
    BracketMatches bracket = rebuildBracket(tournament, bracketSize);

    wireWinnersBracket(bracket);
    wireLosersBracket(bracket);
    seedFirstRound(bracket.winners().getFirst(), seededTeams, bracketSize);
    validateByes(bracket.winners().getFirst(), seededTeams.size(), bracketSize);
    autoAdvanceFirstRoundByes(bracket.winners().getFirst());
    autoAdvanceResolvedByes(tournament);
    resetTeamState(seededTeams);
  }

  private BracketMatches rebuildBracket(Tournament tournament, int bracketSize) {
    int winnersRoundCount = Integer.numberOfTrailingZeros(bracketSize);
    int losersRoundCount = 2 * (winnersRoundCount - 1);

    ensureRoundCount(tournament, BracketType.WINNERS, winnersRoundCount);
    ensureRoundCount(tournament, BracketType.LOSERS, losersRoundCount);
    ensureRoundCount(tournament, BracketType.FINAL, 1);
    removeUnusedRounds(tournament, winnersRoundCount, losersRoundCount);

    List<List<Match>> winners =
        rebuildMatches(
            sortedRounds(tournament, BracketType.WINNERS),
            roundIndex -> bracketSize / (1 << (roundIndex + 1)));
    List<List<Match>> losers =
        rebuildMatches(
            sortedRounds(tournament, BracketType.LOSERS),
            roundIndex -> bracketSize / (1 << ((roundIndex / 2) + 2)));
    Match championship =
        rebuildMatches(sortedRounds(tournament, BracketType.FINAL), roundIndex -> 1)
            .getFirst()
            .getFirst();

    return new BracketMatches(winners, losers, championship);
  }

  private void ensureRoundCount(Tournament tournament, BracketType bracketType, int roundCount) {
    Map<Integer, TournamentRound> roundsByNumber =
        tournament.getRounds().stream()
            .filter(round -> round.getBracketType() == bracketType)
            .collect(
                Collectors.toMap(
                    TournamentRound::getRoundNumber, Function.identity()));

    for (int roundNumber = 1; roundNumber <= roundCount; roundNumber++) {
      if (roundsByNumber.containsKey(roundNumber)) {
        continue;
      }
      tournament
          .getRounds()
          .add(
              TournamentRound.builder()
                  .tournament(tournament)
                  .bracketType(bracketType)
                  .roundNumber(roundNumber)
                  .bestOf(1)
                  .scoringMode(ScoringMode.STANDARD)
                  .build());
    }
  }

  private void removeUnusedRounds(
      Tournament tournament, int winnersRoundCount, int losersRoundCount) {
    tournament
        .getRounds()
        .removeIf(
            round ->
                switch (round.getBracketType()) {
                  case WINNERS -> round.getRoundNumber() > winnersRoundCount;
                  case LOSERS -> round.getRoundNumber() > losersRoundCount;
                  case FINAL -> round.getRoundNumber() > 1;
                  case GRAND_FINAL -> true;
                });
  }

  private List<TournamentRound> sortedRounds(
      Tournament tournament, BracketType bracketType) {
    return tournament.getRounds().stream()
        .filter(round -> round.getBracketType() == bracketType)
        .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
        .toList();
  }

  private List<List<Match>> rebuildMatches(
      List<TournamentRound> rounds, MatchCountByRound matchCountByRound) {
    List<List<Match>> matchesByRound = new ArrayList<>(rounds.size());
    for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
      TournamentRound round = rounds.get(roundIndex);
      round.getMatches().clear();
      List<Match> matches = createMatches(round, matchCountByRound.count(roundIndex));
      round.getMatches().addAll(matches);
      matchesByRound.add(matches);
    }
    return matchesByRound;
  }

  private List<Match> createMatches(TournamentRound round, int matchCount) {
    List<Match> matches = new ArrayList<>(matchCount);
    for (int matchNumber = 1; matchNumber <= matchCount; matchNumber++) {
      matches.add(
          Match.builder()
              .round(round)
              .matchNumber(matchNumber)
              .status(MatchStatus.PENDING)
              .build());
    }
    return matches;
  }

  private void wireWinnersBracket(BracketMatches bracket) {
    List<List<Match>> winners = bracket.winners();
    for (int roundIndex = 0; roundIndex < winners.size(); roundIndex++) {
      List<Match> currentRound = winners.get(roundIndex);
      boolean winnersFinal = roundIndex == winners.size() - 1;

      for (int matchIndex = 0; matchIndex < currentRound.size(); matchIndex++) {
        Match match = currentRound.get(matchIndex);
        if (winnersFinal) {
          routeWinner(match, bracket.championship(), 1);
        } else {
          routeWinner(
              match,
              winners.get(roundIndex + 1).get(matchIndex / 2),
              (matchIndex % 2) + 1);
        }
        routeWinnersBracketLoser(match, bracket.losers(), roundIndex, matchIndex);
      }
    }
  }

  private void routeWinnersBracketLoser(
      Match match, List<List<Match>> losers, int winnersRoundIndex, int matchIndex) {
    if (winnersRoundIndex == 0) {
      routeLoser(match, losers.getFirst().get(matchIndex / 2), (matchIndex % 2) + 1);
      return;
    }

    int losersRoundIndex = (2 * winnersRoundIndex) - 1;
    List<Match> destinationRound = losers.get(losersRoundIndex);
    int destinationIndex = destinationRound.size() - 1 - matchIndex;
    routeLoser(match, destinationRound.get(destinationIndex), 2);
  }

  private void wireLosersBracket(BracketMatches bracket) {
    List<List<Match>> losers = bracket.losers();
    for (int roundIndex = 0; roundIndex < losers.size(); roundIndex++) {
      List<Match> currentRound = losers.get(roundIndex);
      boolean losersFinal = roundIndex == losers.size() - 1;

      for (int matchIndex = 0; matchIndex < currentRound.size(); matchIndex++) {
        Match match = currentRound.get(matchIndex);
        if (losersFinal) {
          routeWinner(match, bracket.championship(), 2);
        } else if (roundIndex % 2 == 0) {
          routeWinner(match, losers.get(roundIndex + 1).get(matchIndex), 1);
        } else {
          routeWinner(
              match,
              losers.get(roundIndex + 1).get(matchIndex / 2),
              (matchIndex % 2) + 1);
        }
      }
    }
  }

  private void routeWinner(Match source, Match destination, int position) {
    source.setWinnerNextMatch(destination);
    source.setWinnerNextMatchPosition(position);
  }

  private void routeLoser(Match source, Match destination, int position) {
    source.setLoserNextMatch(destination);
    source.setLoserNextMatchPosition(position);
  }

  private void seedFirstRound(
      List<Match> firstRoundMatches, List<TournamentTeam> seededTeams, int bracketSize) {
    TournamentTeam[] bracketSlots = new TournamentTeam[bracketSize];
    for (TournamentTeam team : seededTeams) {
      bracketSlots[team.getSeed() - 1] = team;
    }

    for (int matchIndex = 0; matchIndex < firstRoundMatches.size(); matchIndex++) {
      TournamentTeam seedTop = bracketSlots[matchIndex];
      TournamentTeam seedBottom = bracketSlots[bracketSize - 1 - matchIndex];
      configureSeededMatch(firstRoundMatches.get(matchIndex), seedTop, seedBottom);
    }
  }

  private void validateSeeds(List<TournamentTeam> seededTeams, int bracketSize) {
    Set<Integer> assignedSeeds = new HashSet<>();
    for (TournamentTeam team : seededTeams) {
      int seed = team.getSeed() != null ? team.getSeed() : 0;
      if (seed <= 0 || seed > bracketSize || !assignedSeeds.add(seed)) {
        throw new InvalidTournamentStateException(
            "Invalid seed assignment while generating bracket");
      }
    }
  }

  private void configureSeededMatch(
      Match match, TournamentTeam seedTop, TournamentTeam seedBottom) {
    match.setTeamOneWins(0);
    match.setTeamTwoWins(0);
    match.setWinner(null);

    if (seedTop != null && seedBottom != null) {
      match.setTeamOne(seedTop);
      match.setTeamTwo(seedBottom);
      match.setBye(false);
      match.setStatus(MatchStatus.PENDING);
      return;
    }

    TournamentTeam advancingTeam = seedTop != null ? seedTop : seedBottom;
    match.setTeamOne(advancingTeam);
    match.setTeamTwo(null);
    match.setBye(true);
    match.setStatus(MatchStatus.COMPLETED);
    match.setWinner(advancingTeam);
  }

  private void validateByes(List<Match> firstRoundMatches, int teamCount, int bracketSize) {
    int expectedByeCount = bracketSize - teamCount;
    List<Match> byeMatches = firstRoundMatches.stream().filter(Match::isBye).toList();
    if (byeMatches.size() != expectedByeCount) {
      throw new InvalidTournamentStateException("Invalid number of bye matches generated");
    }

    List<Integer> byeSeeds =
        byeMatches.stream()
            .map(Match::getWinner)
            .map(TournamentTeam::getSeed)
            .sorted()
            .toList();
    List<Integer> expectedSeeds = IntStream.rangeClosed(1, expectedByeCount).boxed().toList();
    if (!byeSeeds.equals(expectedSeeds)) {
      throw new InvalidTournamentStateException("Byes must be assigned to top seeds");
    }
  }

  private void autoAdvanceFirstRoundByes(List<Match> firstRoundMatches) {
    firstRoundMatches.stream()
        .filter(Match::isBye)
        .filter(match -> match.getWinner() != null)
        .forEach(
            match ->
                assignTeam(
                    match.getWinner(),
                    match.getWinnerNextMatch(),
                    match.getWinnerNextMatchPosition()));
  }

  private void autoAdvanceResolvedByes(Tournament tournament) {
    List<Match> matches =
        tournament.getRounds().stream().flatMap(round -> round.getMatches().stream()).toList();

    boolean advanced;
    do {
      advanced = false;
      for (Match match : matches) {
        if (match.getStatus() != MatchStatus.PENDING || !allSourcesCompleted(matches, match)) {
          continue;
        }

        TournamentTeam onlyTeam = onlyTeam(match);
        if (teamCount(match) > 1) {
          continue;
        }

        match.setBye(true);
        match.setStatus(MatchStatus.COMPLETED);
        match.setWinner(onlyTeam);
        if (onlyTeam != null) {
          assignTeam(onlyTeam, match.getWinnerNextMatch(), match.getWinnerNextMatchPosition());
        }
        advanced = true;
      }
    } while (advanced);
  }

  private boolean allSourcesCompleted(List<Match> matches, Match destination) {
    List<Match> sources =
        matches.stream()
            .filter(
                source ->
                    source.getWinnerNextMatch() == destination
                        || source.getLoserNextMatch() == destination)
            .toList();
    return !sources.isEmpty()
        && sources.stream().allMatch(source -> source.getStatus() == MatchStatus.COMPLETED);
  }

  private int teamCount(Match match) {
    int count = 0;
    if (match.getTeamOne() != null) {
      count++;
    }
    if (match.getTeamTwo() != null) {
      count++;
    }
    return count;
  }

  private TournamentTeam onlyTeam(Match match) {
    if (match.getTeamOne() != null && match.getTeamTwo() == null) {
      return match.getTeamOne();
    }
    if (match.getTeamTwo() != null && match.getTeamOne() == null) {
      return match.getTeamTwo();
    }
    return null;
  }

  private void assignTeam(TournamentTeam team, Match destination, Integer position) {
    if (destination == null || position == null) {
      return;
    }
    if (position == 1) {
      destination.setTeamOne(team);
    } else if (position == 2) {
      destination.setTeamTwo(team);
    }
  }

  private void resetTeamState(List<TournamentTeam> teams) {
    teams.forEach(
        team -> {
          team.setLosses(0);
          team.setEliminated(false);
        });
  }

  private int calculateBracketSize(int teamCount) {
    int bracketSize = 1;
    while (bracketSize < teamCount) {
      bracketSize *= 2;
    }
    return bracketSize;
  }

  private record BracketMatches(
      List<List<Match>> winners, List<List<Match>> losers, Match championship) {}

  @FunctionalInterface
  private interface MatchCountByRound {
    int count(int roundIndex);
  }
}
