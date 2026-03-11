package com.github.solisa14.fourbagger.api.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateGameRequest(
    @NotNull UUID playerTwoId, @Min(11) @Max(21) Integer targetScore, Boolean winByTwo) {

  public int resolvedTargetScore() {
    return targetScore != null ? targetScore : 21;
  }

  public boolean resolvedWinByTwo() {
    return winByTwo != null && winByTwo;
  }
}
