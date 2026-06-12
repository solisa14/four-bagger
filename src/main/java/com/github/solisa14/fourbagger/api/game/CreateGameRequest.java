package com.github.solisa14.fourbagger.api.game;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request payload for creating a new standalone game. */
public record CreateGameRequest(
    @NotNull UUID playerTwoId,
    UUID playerOnePartnerId,
    UUID playerTwoPartnerId,
    GameType gameType) {

  public CreateGameRequest(UUID playerTwoId) {
    this(playerTwoId, null, null, null);
  }
}
