package com.github.solisa14.fourbagger.api.game;

class StandardGameScoringPolicy implements GameScoringPolicy {

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

  @Override
  public void applyFrame(Game game, int playerOneFramePoints, int playerTwoFramePoints) {
    game.setPlayerOneScore(game.getPlayerOneScore() + playerOneFramePoints);
    game.setPlayerTwoScore(game.getPlayerTwoScore() + playerTwoFramePoints);
    setWinnerIfSatisfied(game, true);
  }
}
