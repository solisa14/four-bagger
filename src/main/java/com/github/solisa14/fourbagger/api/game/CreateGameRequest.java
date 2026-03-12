package com.github.solisa14.fourbagger.api.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateGameRequest(
    @NotNull UUID playerTwoId,
    UUID playerOnePartnerId,
    UUID playerTwoPartnerId,
    GameType gameType,
    GameScoringMode scoringMode,
    @Min(11) @Max(21) Integer targetScore,
    Boolean winByTwo) {

  public CreateGameRequest(
      UUID playerTwoId,
      UUID playerOnePartnerId,
      UUID playerTwoPartnerId,
      GameType gameType,
      Integer targetScore,
      Boolean winByTwo) {
    this(
        playerTwoId, playerOnePartnerId, playerTwoPartnerId, gameType, null, targetScore, winByTwo);
  }

  public CreateGameRequest(UUID playerTwoId, Integer targetScore, Boolean winByTwo) {
    this(playerTwoId, null, null, null, null, targetScore, winByTwo);
  }
}
