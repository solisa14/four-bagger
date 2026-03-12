package com.github.solisa14.fourbagger.api.tournament;

/** Represents the current state of a tournament match. */
public enum MatchStatus {
  /** The match has been scheduled but has not yet started. */
  PENDING,

  /** The match is currently being played. */
  IN_PROGRESS,

  /** The match has finished and a winner has been determined. */
  COMPLETED
}
