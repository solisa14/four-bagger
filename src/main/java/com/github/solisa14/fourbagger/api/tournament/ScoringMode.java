package com.github.solisa14.fourbagger.api.tournament;

/** Determines the scoring rules applied to games within a tournament. */
public enum ScoringMode {
  /** Standard cornhole scoring rules. */
  STANDARD,

  /**
   * Exact scoring rules where exceeding a target score may incur a penalty or require an exact hit.
   */
  EXACT
}
