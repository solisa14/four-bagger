package com.github.solisa14.fourbagger.api.tournament;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request payload for submitting a tournament physical game result. */
public record SubmitTournamentGameResultRequest(
    @NotNull UUID winnerTeamId,
    @NotNull @Min(0) Integer teamOneScore,
    @NotNull @Min(0) Integer teamTwoScore) {}
