package com.github.solisa14.fourbagger.api.tournament;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request payload for an organizer-only tournament match result override. */
public record OverrideTournamentMatchResultRequest(@NotNull UUID winnerTeamId) {}
