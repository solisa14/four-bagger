package com.github.solisa14.fourbagger.api.tournament;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Advances tournament state after a tournament game result is applied. */
@Service
class TournamentProgressionService {

  private final MatchRepository matchRepository;
  private final SingleEliminationProgressionHandler singleEliminationProgressionHandler;
  private final DoubleEliminationProgressionHandler doubleEliminationProgressionHandler;

  TournamentProgressionService(
      MatchRepository matchRepository,
      SingleEliminationProgressionHandler singleEliminationProgressionHandler,
      DoubleEliminationProgressionHandler doubleEliminationProgressionHandler) {
    this.matchRepository = matchRepository;
    this.singleEliminationProgressionHandler = singleEliminationProgressionHandler;
    this.doubleEliminationProgressionHandler = doubleEliminationProgressionHandler;
  }

  @Transactional
  void applyGameResult(TournamentGameResult result) {
    Match match = result.getMatch();
    if (match.getStatus() == MatchStatus.COMPLETED) {
      return;
    }

    TournamentTeam winningTeam = result.getWinnerTeam();
    validateWinnerTeam(match, winningTeam);
    incrementWins(match, winningTeam);

    if (isSeriesClinched(match)) {
      TournamentTeam losingTeam = resolveLosingTeam(match, winningTeam);
      completeMatch(match, winningTeam, losingTeam);
    } else {
      match.setStatus(MatchStatus.IN_PROGRESS);
      matchRepository.save(match);
    }
  }

  int winsToClinch(Match match) {
    return (match.getRound().getBestOf() / 2) + 1;
  }

  boolean isSeriesClinched(Match match) {
    int winsToClinch = winsToClinch(match);
    return match.getTeamOneWins() >= winsToClinch || match.getTeamTwoWins() >= winsToClinch;
  }

  Integer nextGameNumber(Match match) {
    if (match.getStatus() == MatchStatus.COMPLETED || isSeriesClinched(match)) {
      return null;
    }
    return match.getTeamOneWins() + match.getTeamTwoWins() + 1;
  }

  private void validateWinnerTeam(Match match, TournamentTeam winningTeam) {
    UUID winnerId = winningTeam.getId();
    if ((match.getTeamOne() == null || !match.getTeamOne().getId().equals(winnerId))
        && (match.getTeamTwo() == null || !match.getTeamTwo().getId().equals(winnerId))) {
      throw new InvalidTournamentStateException("Winner team is not a participant in the linked match");
    }
  }

  private void incrementWins(Match match, TournamentTeam winningTeam) {
    if (winningTeam.getId().equals(match.getTeamOne().getId())) {
      match.setTeamOneWins(match.getTeamOneWins() + 1);
    } else if (match.getTeamTwo() != null
        && winningTeam.getId().equals(match.getTeamTwo().getId())) {
      match.setTeamTwoWins(match.getTeamTwoWins() + 1);
    }
    matchRepository.save(match);
  }

  private TournamentTeam resolveLosingTeam(Match match, TournamentTeam winningTeam) {
    if (winningTeam.getId().equals(match.getTeamOne().getId())) {
      return match.getTeamTwo();
    }
    return match.getTeamOne();
  }

  private void completeMatch(
      Match match, TournamentTeam winningTeam, TournamentTeam losingTeam) {
    match.setWinner(winningTeam);
    match.setStatus(MatchStatus.COMPLETED);
    matchRepository.save(match);

    Tournament tournament = match.getRound().getTournament();
    TournamentFormat format =
        tournament.getFormat() != null
            ? tournament.getFormat()
            : TournamentFormat.SINGLE_ELIMINATION;
    if (format == TournamentFormat.SINGLE_ELIMINATION) {
      singleEliminationProgressionHandler.progress(match, winningTeam, losingTeam);
    } else if (format == TournamentFormat.DOUBLE_ELIMINATION) {
      doubleEliminationProgressionHandler.progress(match, winningTeam, losingTeam);
    } else {
      throw new InvalidTournamentStateException("Unsupported tournament format: " + format);
    }
  }
}
