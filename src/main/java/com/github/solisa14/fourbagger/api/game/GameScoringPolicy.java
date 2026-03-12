package com.github.solisa14.fourbagger.api.game;

/**
 * Defines the contract for applying frame scores to a game. Different scoring modes (like exact or
 * standard) will have their own implementation of this interface.
 */
public interface GameScoringPolicy {

  /**
   * Applies the calculated points for a single frame to the game's total scores based on the
   * specific rules of the scoring policy.
   *
   * @param game The game instance to update.
   * @param playerOneFramePoints The net points scored by player one in this frame.
   * @param playerTwoFramePoints The net points scored by player two in this frame.
   */
  void applyFrame(Game game, int playerOneFramePoints, int playerTwoFramePoints);
}
