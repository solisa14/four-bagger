package com.github.solisa14.fourbagger.api.tournament;

/** Applies format-specific bracket changes after a tournament match is completed. */
interface TournamentProgressionHandler {

  void progress(Match match, TournamentTeam winningTeam, TournamentTeam losingTeam);
}
