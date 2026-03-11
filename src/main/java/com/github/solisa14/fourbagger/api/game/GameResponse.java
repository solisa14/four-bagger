package com.github.solisa14.fourbagger.api.game;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameResponse(
    UUID id,
    GameType gameType,
    PlayerInfo playerOne,
    PlayerInfo playerOnePartner,
    PlayerInfo playerTwo,
    PlayerInfo playerTwoPartner,
    int playerOneScore,
    int playerTwoScore,
    int targetScore,
    boolean winByTwo,
    GameStatus status,
    PlayerInfo winner,
    List<FrameResponse> frames,
    Instant createdAt,
    Instant updatedAt) {

  public static GameResponse from(Game game) {
    return new GameResponse(
        game.getId(),
        game.getGameType(),
        PlayerInfo.from(game.getPlayerOne()),
        game.getPlayerOnePartner() != null ? PlayerInfo.from(game.getPlayerOnePartner()) : null,
        PlayerInfo.from(game.getPlayerTwo()),
        game.getPlayerTwoPartner() != null ? PlayerInfo.from(game.getPlayerTwoPartner()) : null,
        game.getPlayerOneScore(),
        game.getPlayerTwoScore(),
        game.getTargetScore(),
        game.isWinByTwo(),
        game.getStatus(),
        game.getWinner() != null ? PlayerInfo.from(game.getWinner()) : null,
        game.getFrames().stream().map(FrameResponse::from).toList(),
        game.getCreatedAt(),
        game.getUpdatedAt());
  }
}
