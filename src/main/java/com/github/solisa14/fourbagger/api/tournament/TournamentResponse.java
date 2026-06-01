package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing the public information of a tournament.
 *
 * @param id the unique identifier of the tournament
 * @param title the title of the tournament
 * @param joinCode the 6-character unique join code
 * @param status the current status of the tournament
 * @param gameType whether the tournament is SINGLES or DOUBLES
 * @param rounds the tournament rounds including bracket structure and match state, ordered by round
 *     number; empty when the bracket has not yet been generated
 */
public record TournamentResponse(
    UUID id,
    String title,
    String joinCode,
    TournamentStatus status,
    GameType gameType,
    List<TournamentRoundResponse> rounds) {}
