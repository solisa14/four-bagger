package com.github.solisa14.fourbagger.api.game;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing the full details of a game. Includes participant info, current
 * scores, status, and the list of frames.
 *
 * @param id The unique identifier of the game.
 * @param gameType The type of game (e.g., SINGLES, DOUBLES).
 * @param playerOne The primary player for team one.
 * @param playerOnePartner The partner for team one (if doubles).
 * @param playerTwo The primary player for team two.
 * @param playerTwoPartner The partner for team two (if doubles).
 * @param playerOneScore The current score for team one.
 * @param playerTwoScore The current score for team two.
 * @param targetScore The score required to win the game.
 * @param status The current status of the game.
 * @param winner The player or team that won the game.
 * @param frames The sequence of frames played in the game.
 * @param createdAt The timestamp when the game was created.
 * @param updatedAt The timestamp when the game was last updated.
 */
public record GameResponse(
    UUID id,
    GameType gameType,
    PlayerInfo playerOne,
    PlayerInfo playerOnePartner,
    PlayerInfo playerTwo,
    PlayerInfo playerTwoPartner,
    int playerOneScore,
    int playerTwoScore,
    int targetScore,
    GameStatus status,
    PlayerInfo winner,
    List<FrameResponse> frames,
    Instant createdAt,
    Instant updatedAt) {

}
