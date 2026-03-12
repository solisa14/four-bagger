package com.github.solisa14.fourbagger.api.tournament;

/** Represents the lifecycle stages of a tournament. */
public enum TournamentStatus {
  /** The tournament is open for participants to join. */
  REGISTRATION,

  /** Registration is closed, and the tournament bracket has been generated and finalized. */
  BRACKET_READY,

  /** Matches are currently being played in the tournament. */
  IN_PROGRESS,

  /** All matches have concluded, and the tournament is over. */
  COMPLETED
}
