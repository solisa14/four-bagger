package com.github.solisa14.fourbagger.api.game;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request payload for submitting a standalone game's final result. */
public record SubmitGameResultRequest(
    @NotNull UUID winnerUserId,
    @NotNull @Min(0) Integer playerOneScore,
    @NotNull @Min(0) Integer playerTwoScore) {}
