package com.github.solisa14.fourbagger.api.tournament;

import java.util.UUID;

/**
 * Data Transfer Object representing the public information of a tournament.
 *
 * @param id the unique identifier of the tournament
 * @param title the title of the tournament
 * @param joinCode the 6-character unique join code
 * @param status the current status of the tournament
 */
public record TournamentResponse(UUID id, String title, String joinCode, TournamentStatus status) {}
