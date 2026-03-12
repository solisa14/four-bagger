package com.github.solisa14.fourbagger.api.game;

class ExactGameScoringPolicy implements GameScoringPolicy {

  private static final int EXACT_BUST_RESET_SCORE = 11;

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
