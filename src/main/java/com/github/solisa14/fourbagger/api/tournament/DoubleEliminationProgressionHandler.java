package com.github.solisa14.fourbagger.api.tournament;

import org.springframework.stereotype.Service;

/**
 * Advances both teams through a double-elimination graph and eliminates a team after its second
 * loss.
 */
@Service
class DoubleEliminationProgressionHandler implements TournamentProgressionHandler {

  private static final int LOSSES_TO_ELIMINATE = 2;

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;

  DoubleEliminationProgressionHandler(
      TournamentRepository tournamentRepository, MatchRepository matchRepository) {
    this.tournamentRepository = tournamentRepository;
    this.matchRepository = matchRepository;
  }

  @Override
  public void progress(Match match, TournamentTeam winningTeam, TournamentTeam losingTeam) {
    losingTeam.setLosses(losingTeam.getLosses() + 1);
    losingTeam.setEliminated(losingTeam.getLosses() >= LOSSES_TO_ELIMINATE);

    if (isFirstFinal(match)) {
      progressFirstFinal(match, winningTeam, losingTeam);
      return;
    }

    routeTeam(winningTeam, match.getWinnerNextMatch(), match.getWinnerNextMatchPosition());
    routeTeam(losingTeam, match.getLoserNextMatch(), match.getLoserNextMatchPosition());

    saveDestinationMatches(match);
    if (match.getWinnerNextMatch() == null && match.getLoserNextMatch() == null) {
      completeTournament(match.getRound().getTournament());
    }
  }

  void revert(Match match, TournamentTeam oldWinner, TournamentTeam oldLoser) {
    if (oldLoser != null) {
      oldLoser.setLosses(Math.max(0, oldLoser.getLosses() - 1));
      if (oldLoser.getLosses() < LOSSES_TO_ELIMINATE) {
        oldLoser.setEliminated(false);
      }
    }

    if (isFirstFinal(match)) {
      revertFirstFinal(match, oldWinner, oldLoser);
      return;
    }

    removeTeamFromSlot(oldWinner, match.getWinnerNextMatch(), match.getWinnerNextMatchPosition());
    removeTeamFromSlot(oldLoser, match.getLoserNextMatch(), match.getLoserNextMatchPosition());
    saveDestinationMatches(match);

    if (match.getWinnerNextMatch() == null && match.getLoserNextMatch() == null) {
      reopenTournamentIfCompleted(match.getRound().getTournament());
    }
  }

  private void progressFirstFinal(
      Match match, TournamentTeam winningTeam, TournamentTeam losingTeam) {
    if (losingTeam.isEliminated()) {
      completeTournament(match.getRound().getTournament());
      return;
    }

    Match resetFinal = match.getWinnerNextMatch();
    routeTeam(winningTeam, resetFinal, match.getWinnerNextMatchPosition());
    routeTeam(losingTeam, match.getLoserNextMatch(), match.getLoserNextMatchPosition());
    resetFinal.getRound().setBestOf(match.getRound().getBestOf());
    matchRepository.save(resetFinal);
  }

  private boolean isFirstFinal(Match match) {
    return match.getRound().getBracketType() == BracketType.FINAL
        && match.getWinnerNextMatch() != null
        && match.getWinnerNextMatch().getRound().getBracketType() == BracketType.GRAND_FINAL;
  }

  private void revertFirstFinal(Match match, TournamentTeam oldWinner, TournamentTeam oldLoser) {
    Match resetFinal = match.getWinnerNextMatch();
    if (resetFinal == null) {
      reopenTournamentIfCompleted(match.getRound().getTournament());
      return;
    }

    if (resetFinal.getTeamOne() != null || resetFinal.getTeamTwo() != null) {
      removeTeamFromSlot(oldWinner, resetFinal, match.getWinnerNextMatchPosition());
      removeTeamFromSlot(oldLoser, resetFinal, match.getLoserNextMatchPosition());
      matchRepository.save(resetFinal);
      return;
    }

    reopenTournamentIfCompleted(match.getRound().getTournament());
  }

  private void routeTeam(TournamentTeam team, Match destination, Integer position) {
    if (destination == null || position == null) {
      return;
    }
    if (position == 1) {
      destination.setTeamOne(team);
    } else if (position == 2) {
      destination.setTeamTwo(team);
    }
  }

  private void removeTeamFromSlot(TournamentTeam team, Match destination, Integer position) {
    if (destination == null || team == null || position == null) {
      return;
    }
    if (position == 1 && destination.getTeamOne() != null
        && destination.getTeamOne().getId().equals(team.getId())) {
      destination.setTeamOne(null);
    } else if (position == 2 && destination.getTeamTwo() != null
        && destination.getTeamTwo().getId().equals(team.getId())) {
      destination.setTeamTwo(null);
    }
  }

  private void saveDestinationMatches(Match source) {
    Match winnerDestination = source.getWinnerNextMatch();
    Match loserDestination = source.getLoserNextMatch();
    if (winnerDestination != null) {
      matchRepository.save(winnerDestination);
    }
    if (loserDestination != null && loserDestination != winnerDestination) {
      matchRepository.save(loserDestination);
    }
  }

  private void completeTournament(Tournament tournament) {
    tournament.setStatus(TournamentStatus.COMPLETED);
    tournamentRepository.save(tournament);
  }

  private void reopenTournamentIfCompleted(Tournament tournament) {
    if (tournament.getStatus() == TournamentStatus.COMPLETED) {
      tournament.setStatus(TournamentStatus.IN_PROGRESS);
      tournamentRepository.save(tournament);
    }
  }
}
