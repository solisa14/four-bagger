package com.github.solisa14.fourbagger.api.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordFrameRequest(
    @NotNull @Min(0) @Max(4) Integer p1BagsIn,
    @NotNull @Min(0) @Max(4) Integer p1BagsOn,
    @NotNull @Min(0) @Max(4) Integer p2BagsIn,
    @NotNull @Min(0) @Max(4) Integer p2BagsOn) {}
