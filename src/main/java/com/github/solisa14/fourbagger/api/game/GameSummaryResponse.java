package com.github.solisa14.fourbagger.api.game;

import java.time.Instant;
import java.util.UUID;

public record GameSummaryResponse(
    UUID id,
    PlayerInfo playerOne,
    PlayerInfo playerTwo,
    int playerOneScore,
    int playerTwoScore,
    int targetScore,
    boolean winByTwo,
    GameStatus status,
    PlayerInfo winner,
    Instant createdAt,
    Instant updatedAt) {

  public static GameSummaryResponse from(Game game) {
    return new GameSummaryResponse(
        game.getId(),
        PlayerInfo.from(game.getPlayerOne()),
        PlayerInfo.from(game.getPlayerTwo()),
        game.getPlayerOneScore(),
        game.getPlayerTwoScore(),
        game.getTargetScore(),
        game.isWinByTwo(),
        game.getStatus(),
        game.getWinner() != null ? PlayerInfo.from(game.getWinner()) : null,
        game.getCreatedAt(),
        game.getUpdatedAt());
  }
}
