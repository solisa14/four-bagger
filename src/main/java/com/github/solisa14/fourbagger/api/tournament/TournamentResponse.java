package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import java.util.UUID;

/**
 * Data Transfer Object representing the public information of a tournament.
 *
 * @param id the unique identifier of the tournament
 * @param title the title of the tournament
 * @param joinCode the 6-character unique join code
 * @param status the current status of the tournament
 * @param gameType whether the tournament is SINGLES or DOUBLES
 * @param brackets the tournament rounds grouped by bracket section; each group is ordered by round
 *     number and empty when absent
 */
public record TournamentResponse(
    UUID id,
    String title,
    String joinCode,
    TournamentStatus status,
    GameType gameType,
    TournamentFormat format,
    TournamentBracketsResponse brackets) {}
