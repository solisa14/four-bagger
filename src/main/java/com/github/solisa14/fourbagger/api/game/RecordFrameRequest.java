package com.github.solisa14.fourbagger.api.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecordFrameRequest(
    @NotNull @Min(0) @Max(4) Integer p1BagsIn,
    @NotNull @Min(0) @Max(4) Integer p1BagsOn,
    @NotNull @Min(0) @Max(4) Integer p2BagsIn,
    @NotNull @Min(0) @Max(4) Integer p2BagsOn,
    UUID playerOneThrowerId,
    UUID playerTwoThrowerId) {

  public RecordFrameRequest(
      Integer p1BagsIn, Integer p1BagsOn, Integer p2BagsIn, Integer p2BagsOn) {
    this(p1BagsIn, p1BagsOn, p2BagsIn, p2BagsOn, null, null);
  }
}
