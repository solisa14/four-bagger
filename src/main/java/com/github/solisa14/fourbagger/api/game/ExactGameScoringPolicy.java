package com.github.solisa14.fourbagger.api.game;

/**
 * Implementation of {@link GameScoringPolicy} for the "Exact" scoring mode. In this mode, players
 * must reach the target score exactly. If their score exceeds the target, their score resets to a
 * predefined value (typically 11).
 */
class ExactGameScoringPolicy implements GameScoringPolicy {

  /** The score a player gets reset to if they exceed the target score. */
  private static final int EXACT_BUST_RESET_SCORE = 11;

  /**
   * Applies the frame points to the game's scores. If a player exceeds the target score, their
   * score is reset to {@link #EXACT_BUST_RESET_SCORE}. Then checks if there is a winner.
   *
   * @param game The game to update.
   * @param playerOneFramePoints Points scored by player one in the frame.
   * @param playerTwoFramePoints Points scored by player two in the frame.
   */
  @Override
  public void applyFrame(Game game, int playerOneFramePoints, int playerTwoFramePoints) {
    game.setPlayerOneScore(game.getPlayerOneScore() + playerOneFramePoints);
    game.setPlayerTwoScore(game.getPlayerTwoScore() + playerTwoFramePoints);

    if (game.getPlayerOneScore() > game.getTargetScore()) {
      game.setPlayerOneScore(EXACT_BUST_RESET_SCORE);
    }
    if (game.getPlayerTwoScore() > game.getTargetScore()) {
      game.setPlayerTwoScore(EXACT_BUST_RESET_SCORE);
    }

    StandardGameScoringPolicy.setWinnerIfSatisfied(game, false);
  }
}
