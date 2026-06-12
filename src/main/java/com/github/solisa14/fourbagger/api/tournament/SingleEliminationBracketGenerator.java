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
    List<List<Match>> matchesByRound = rebuildMatchesByRound(tournament, bracketSize);
    wireNextMatches(matchesByRound);

    List<Match> firstRoundMatches = matchesByRound.getFirst();
    seedFirstRound(firstRoundMatches, seededTeams, bracketSize);
    validateByes(firstRoundMatches, seededTeams.size(), bracketSize);
    autoAdvanceByes(firstRoundMatches);
  }

  private List<List<Match>> rebuildMatchesByRound(Tournament tournament, int bracketSize) {
    int roundCount = Integer.numberOfTrailingZeros(bracketSize);
    ensureRoundCount(tournament, roundCount);

    List<List<Match>> matchesByRound = new ArrayList<>(roundCount);
    int matchCount = bracketSize / 2;
    for (TournamentRound round : sortedRounds(tournament)) {
      round.getMatches().clear();
      List<Match> matches = createMatches(round, matchCount);
      round.getMatches().addAll(matches);
      matchesByRound.add(matches);
      matchCount /= 2;
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
          TournamentRound.builder()
              .tournament(tournament)
              .bracketType(BracketType.WINNERS)
              .roundNumber(roundNumber)
              .bestOf(1)
              .build();
      tournament.getRounds().add(newRound);
    }
    tournament
        .getRounds()
        .removeIf(
            round ->
                round.getBracketType() == BracketType.WINNERS
                    && round.getRoundNumber() > roundCount);
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
        current.setWinnerNextMatch(winnerNextMatch);
        current.setWinnerNextMatchPosition((matchIndex % 2) + 1);
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
      if (!match.isBye() || match.getWinner() == null || match.getWinnerNextMatch() == null) {
        continue;
      }
      if (match.getWinnerNextMatchPosition() != null && match.getWinnerNextMatchPosition() == 1) {
        match.getWinnerNextMatch().setTeamOne(match.getWinner());
      } else if (match.getWinnerNextMatchPosition() != null
          && match.getWinnerNextMatchPosition() == 2) {
        match.getWinnerNextMatch().setTeamTwo(match.getWinner());
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
