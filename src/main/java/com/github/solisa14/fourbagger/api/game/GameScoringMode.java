package com.github.solisa14.fourbagger.api.game;

/** Defines the available scoring modes for a game. */
public enum GameScoringMode {
  /** Standard scoring where a player wins by reaching or exceeding the target score. */
  STANDARD,

  /**
   * Exact scoring where a player must hit the target score exactly; exceeding it results in a score
   * reset.
   */
  EXACT
}
