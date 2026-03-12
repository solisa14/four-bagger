package com.github.solisa14.fourbagger.api.tournament;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class TournamentBracketService {

  public void planBracket(Tournament tournament, List<TournamentTeam> seededTeams) {
    int bracketSize = calculateBracketSize(seededTeams.size());
    int roundCount = Integer.numberOfTrailingZeros(bracketSize);

    ensureRoundCount(tournament, roundCount);
    List<TournamentRound> rounds = sortedRounds(tournament);
    rounds.forEach(round -> round.getMatches().clear());

    List<List<Match>> matchesByRound = new ArrayList<>();
    for (int roundIndex = 0; roundIndex < roundCount; roundIndex++) {
      TournamentRound round = rounds.get(roundIndex);
      int matchesThisRound = bracketSize / (1 << (roundIndex + 1));
      List<Match> roundMatches = new ArrayList<>(matchesThisRound);
      for (int matchIndex = 0; matchIndex < matchesThisRound; matchIndex++) {
        Match match =
            Match.builder()
                .round(round)
                .matchNumber(matchIndex + 1)
                .status(MatchStatus.PENDING)
                .build();
        roundMatches.add(match);
      }
      round.getMatches().addAll(roundMatches);
      matchesByRound.add(roundMatches);
    }

    wireNextMatches(matchesByRound);
    seedFirstRound(matchesByRound.getFirst(), seededTeams, bracketSize);
    validateByes(matchesByRound.getFirst(), seededTeams.size(), bracketSize);
    autoAdvanceByes(matchesByRound.getFirst());
  }

  private void ensureRoundCount(Tournament tournament, int roundCount) {
    Map<Integer, TournamentRound> roundsByNumber =
        tournament.getRounds().stream()
            .collect(Collectors.toMap(TournamentRound::getRoundNumber, Function.identity()));

    for (int roundNumber = 1; roundNumber <= roundCount; roundNumber++) {
      if (roundsByNumber.containsKey(roundNumber)) {
        continue;
      }
      TournamentRound newRound =
          TournamentRound.builder()
              .tournament(tournament)
              .roundNumber(roundNumber)
              .bestOf(1)
              .scoringMode(ScoringMode.STANDARD)
              .build();
      tournament.getRounds().add(newRound);
    }
    tournament.getRounds().removeIf(round -> round.getRoundNumber() > roundCount);
  }

  private List<TournamentRound> sortedRounds(Tournament tournament) {
    return tournament.getRounds().stream()
        .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
        .toList();
  }

  private void wireNextMatches(List<List<Match>> matchesByRound) {
    for (int roundIndex = 0; roundIndex < matchesByRound.size() - 1; roundIndex++) {
      List<Match> currentRound = matchesByRound.get(roundIndex);
      List<Match> nextRound = matchesByRound.get(roundIndex + 1);
      for (int matchIndex = 0; matchIndex < currentRound.size(); matchIndex++) {
        Match current = currentRound.get(matchIndex);
        Match nextMatch = nextRound.get(matchIndex / 2);
        current.setNextMatch(nextMatch);
        current.setNextMatchPosition((matchIndex % 2) + 1);
      }
    }
  }

  private void seedFirstRound(
      List<Match> firstRoundMatches, List<TournamentTeam> seededTeams, int bracketSize) {
    TournamentTeam[] bracketSlots = new TournamentTeam[bracketSize];
    for (TournamentTeam team : seededTeams) {
      int seed = team.getSeed() != null ? team.getSeed() : 0;
      if (seed <= 0 || seed > bracketSize) {
        throw new InvalidTournamentStateException(
            "Invalid seed assignment while generating bracket");
      }
      bracketSlots[seed - 1] = team;
    }

    for (int matchIndex = 0; matchIndex < firstRoundMatches.size(); matchIndex++) {
      Match match = firstRoundMatches.get(matchIndex);
      TournamentTeam seedTop = bracketSlots[matchIndex];
      TournamentTeam seedBottom = bracketSlots[bracketSize - 1 - matchIndex];
      configureSeededMatch(match, seedTop, seedBottom);
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
    if (advancingTeam == null) {
      match.setBye(false);
      match.setStatus(MatchStatus.PENDING);
      return;
    }
    match.setBye(true);
    match.setStatus(MatchStatus.COMPLETED);
    match.setWinner(advancingTeam);
  }

  private void autoAdvanceByes(List<Match> firstRoundMatches) {
    for (Match match : firstRoundMatches) {
      if (!match.isBye() || match.getWinner() == null || match.getNextMatch() == null) {
        continue;
      }
      if (match.getNextMatchPosition() != null && match.getNextMatchPosition() == 1) {
        match.getNextMatch().setTeamOne(match.getWinner());
      } else if (match.getNextMatchPosition() != null && match.getNextMatchPosition() == 2) {
        match.getNextMatch().setTeamTwo(match.getWinner());
      }
    }
  }

  private void validateByes(List<Match> firstRoundMatches, int teamCount, int bracketSize) {
    int expectedByeCount = bracketSize - teamCount;
    List<Match> byeMatches = firstRoundMatches.stream().filter(Match::isBye).toList();
    if (byeMatches.size() != expectedByeCount) {
      throw new InvalidTournamentStateException("Invalid number of bye matches generated");
    }
    if (expectedByeCount == 0) {
      return;
    }

    List<Integer> byeSeeds =
        byeMatches.stream()
            .map(Match::getWinner)
            .filter(team -> team != null && team.getSeed() != null)
            .map(TournamentTeam::getSeed)
            .sorted()
            .toList();
    List<Integer> expectedSeeds = IntStream.rangeClosed(1, expectedByeCount).boxed().toList();
    if (!byeSeeds.equals(expectedSeeds)) {
      throw new InvalidTournamentStateException("Byes must be assigned to top seeds");
    }
  }

  private int calculateBracketSize(int teamCount) {
    int bracketSize = 1;
    while (bracketSize < teamCount) {
      bracketSize *= 2;
    }
    return bracketSize;
  }
}
