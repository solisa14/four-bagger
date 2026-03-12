package com.github.solisa14.fourbagger.api.game;

public interface GameScoringPolicy {

  void applyFrame(Game game, int playerOneFramePoints, int playerTwoFramePoints);
}
