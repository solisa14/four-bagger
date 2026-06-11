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

    routeTeam(
        winningTeam, match.getWinnerNextMatch(), match.getWinnerNextMatchPosition());
    routeTeam(losingTeam, match.getLoserNextMatch(), match.getLoserNextMatchPosition());

    saveDestinationMatches(match);
    if (match.getWinnerNextMatch() == null && match.getLoserNextMatch() == null) {
      completeTournament(match.getRound().getTournament());
    }
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
}
