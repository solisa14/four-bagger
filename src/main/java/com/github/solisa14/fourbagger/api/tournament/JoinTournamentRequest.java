package com.github.solisa14.fourbagger.api.tournament;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for joining a tournament via its unique join code.
 *
 * @param joinCode the 6-character tournament join code
 */
public record JoinTournamentRequest(@NotBlank String joinCode) {}
