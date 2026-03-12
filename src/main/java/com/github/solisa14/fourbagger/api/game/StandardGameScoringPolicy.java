package com.github.solisa14.fourbagger.api.game;

/**
 * Implementation of {@link GameScoringPolicy} for the "Standard" scoring mode. In this mode,
 * players can win by reaching or exceeding the target score, optionally requiring a win-by-two
 * margin.
 */
class StandardGameScoringPolicy implements GameScoringPolicy {

  /**
   * Checks the current scores of the game to determine if a player has met the winning criteria. If
   * a winner is determined, updates the game's winner and status to COMPLETED.
   *
   * @param game The game to check and potentially update.
   * @param allowGreaterThanTarget If true, a score greater than or equal to the target is a win. If
   *     false, the score must be exactly the target.
   */
  static void setWinnerIfSatisfied(Game game, boolean allowGreaterThanTarget) {
    int playerOneScore = game.getPlayerOneScore();
    int playerTwoScore = game.getPlayerTwoScore();
    int target = game.getTargetScore();
    boolean winByTwo = game.isWinByTwo();

    boolean playerOneTargetMet =
        allowGreaterThanTarget ? playerOneScore >= target : playerOneScore == target;
    boolean playerTwoTargetMet =
        allowGreaterThanTarget ? playerTwoScore >= target : playerTwoScore == target;

    boolean playerOneWins =
        playerOneTargetMet && (!winByTwo || (playerOneScore - playerTwoScore) >= 2);
    boolean playerTwoWins =
        playerTwoTargetMet && (!winByTwo || (playerTwoScore - playerOneScore) >= 2);

    if (playerOneWins) {
      game.setWinner(game.getPlayerOne());
      game.setStatus(GameStatus.COMPLETED);
    } else if (playerTwoWins) {
      game.setWinner(game.getPlayerTwo());
      game.setStatus(GameStatus.COMPLETED);
    }
  }

  /**
   * Applies the frame points to the game's total scores and checks if either player has won under
   * the standard scoring rules.
   *
   * @param game The game to update.
   * @param playerOneFramePoints Points scored by player one in the frame.
   * @param playerTwoFramePoints Points scored by player two in the frame.
   */
  @Override
  public void applyFrame(Game game, int playerOneFramePoints, int playerTwoFramePoints) {
    game.setPlayerOneScore(game.getPlayerOneScore() + playerOneFramePoints);
    game.setPlayerTwoScore(game.getPlayerTwoScore() + playerTwoFramePoints);
    setWinnerIfSatisfied(game, true);
  }
}
