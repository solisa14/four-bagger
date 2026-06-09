package com.github.solisa14.fourbagger.api.tournament;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/**
 * Generates and seeds a single-elimination tournament bracket. Computes necessary rounds, handles
 * byes for non-power-of-two team counts, and links winner routes so winners automatically progress.
 */
@Component
public class SingleEliminationBracketGenerator implements TournamentBracketGenerator {

  @Override
  public TournamentFormat format() {
    return TournamentFormat.SINGLE_ELIMINATION;
  }

  /**
   * Plans the entire bracket layout for a tournament given a list of seeded teams. Calculates the
   * number of required rounds, creates the matches, seeds the first round, assigns byes to top
   * seeds if necessary, and wires matches to progress to the next round.
   *
   * @param tournament the tournament entity being planned
   * @param seededTeams a list of teams ordered by seed
   */
  @Override
  public void planBracket(Tournament tournament, List<TournamentTeam> seededTeams) {
    int bracketSize = calculateBracketSize(seededTeams.size());
    int roundCount = Integer.numberOfTrailingZeros(bracketSize);

    ensureRoundCount(tournament, roundCount);
    List<TournamentRound> rounds = sortedRounds(tournament);
    rounds.forEach(TournamentRound::clearMatches);

    List<List<Match>> matchesByRound = new ArrayList<>();
    for (int roundIndex = 0; roundIndex < roundCount; roundIndex++) {
      TournamentRound round = rounds.get(roundIndex);
      int matchesThisRound = bracketSize / (1 << (roundIndex + 1));
      List<Match> roundMatches = new ArrayList<>(matchesThisRound);
      for (int matchIndex = 0; matchIndex < matchesThisRound; matchIndex++) {
        Match match = Match.create(round, matchIndex + 1);
        roundMatches.add(match);
      }
      round.replaceMatches(roundMatches);
      matchesByRound.add(roundMatches);
    }

    wireNextMatches(matchesByRound);
    seedFirstRound(matchesByRound.getFirst(), seededTeams, bracketSize);
    validateByes(matchesByRound.getFirst(), seededTeams.size(), bracketSize);
    autoAdvanceByes(matchesByRound.getFirst());
  }

  private void ensureRoundCount(Tournament tournament, int roundCount) {
    Map<Integer, TournamentRound> winnersRoundsByNumber =
        tournament.getRounds().stream()
            .filter(round -> round.getBracketType() == BracketType.WINNERS)
            .collect(Collectors.toMap(TournamentRound::getRoundNumber, round -> round));

    for (int roundNumber = 1; roundNumber <= roundCount; roundNumber++) {
      if (winnersRoundsByNumber.containsKey(roundNumber)) {
        continue;
      }
      TournamentRound newRound =
          TournamentRound.create(
              tournament, BracketType.WINNERS, roundNumber, 1, ScoringMode.STANDARD);
      tournament.addRound(newRound);
    }
    tournament.getRounds().stream()
        .filter(
            round ->
                round.getBracketType() == BracketType.WINNERS
                    && round.getRoundNumber() > roundCount)
        .toList()
        .forEach(tournament::removeRound);
  }

  private List<TournamentRound> sortedRounds(Tournament tournament) {
    return tournament.getRounds().stream()
        .filter(round -> round.getBracketType() == BracketType.WINNERS)
        .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
        .toList();
  }

  private void wireNextMatches(List<List<Match>> matchesByRound) {
    for (int roundIndex = 0; roundIndex < matchesByRound.size() - 1; roundIndex++) {
      List<Match> currentRound = matchesByRound.get(roundIndex);
      List<Match> nextRound = matchesByRound.get(roundIndex + 1);
      for (int matchIndex = 0; matchIndex < currentRound.size(); matchIndex++) {
        Match current = currentRound.get(matchIndex);
        Match winnerNextMatch = nextRound.get(matchIndex / 2);
        current.configureWinnerRoute(winnerNextMatch, (matchIndex % 2) + 1);
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
    if (seedTop != null && seedBottom != null) {
      match.resetForSeeding(seedTop, seedBottom);
      return;
    }

    TournamentTeam advancingTeam = seedTop != null ? seedTop : seedBottom;
    if (advancingTeam == null) {
      match.resetForSeeding(null, null);
      return;
    }
    match.markBye(advancingTeam);
  }

  private void autoAdvanceByes(List<Match> firstRoundMatches) {
    for (Match match : firstRoundMatches) {
      if (!match.isBye() || match.getWinner() == null || match.getWinnerNextMatch() == null) {
        continue;
      }
      if (match.getWinnerNextMatchPosition() != null && match.getWinnerNextMatchPosition() == 1) {
        match.getWinnerNextMatch().assignTeam(1, match.getWinner());
      } else if (match.getWinnerNextMatchPosition() != null
          && match.getWinnerNextMatchPosition() == 2) {
        match.getWinnerNextMatch().assignTeam(2, match.getWinner());
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
