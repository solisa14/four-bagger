package com.github.solisa14.fourbagger.api.tournament;

import org.springframework.stereotype.Service;

/** Advances a completed single-elimination match through its configured winner route. */
@Service
class SingleEliminationProgressionHandler implements TournamentProgressionHandler {

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;

  SingleEliminationProgressionHandler(
      TournamentRepository tournamentRepository, MatchRepository matchRepository) {
    this.tournamentRepository = tournamentRepository;
    this.matchRepository = matchRepository;
  }

  @Override
  public void progress(Match match, TournamentTeam winningTeam, TournamentTeam losingTeam) {
    Match destination = match.getWinnerNextMatch();
    if (destination != null) {
      assignTeam(winningTeam, destination, match.getWinnerNextMatchPosition());
      matchRepository.save(destination);
      return;
    }

    completeTournament(match.getRound().getTournament());
  }

  private void assignTeam(TournamentTeam team, Match destination, Integer position) {
    if (position != null && position == 1) {
      destination.setTeamOne(team);
    } else if (position != null && position == 2) {
      destination.setTeamTwo(team);
    }
  }

  private void completeTournament(Tournament tournament) {
    tournament.setStatus(TournamentStatus.COMPLETED);
    tournamentRepository.save(tournament);
  }
}
