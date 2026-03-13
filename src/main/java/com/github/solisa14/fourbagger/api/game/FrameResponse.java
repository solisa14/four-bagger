package com.github.solisa14.fourbagger.api.game;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing the details of a frame.
 *
 * @param id The unique identifier of the frame.
 * @param frameNumber The sequential number of the frame in the game.
 * @param playerOneBagsIn Number of bags player one threw into the hole.
 * @param playerOneBagsOn Number of bags player one threw onto the board.
 * @param playerTwoBagsIn Number of bags player two threw into the hole.
 * @param playerTwoBagsOn Number of bags player two threw onto the board.
 * @param playerOneFramePoints Net points scored by player one in this frame.
 * @param playerTwoFramePoints Net points scored by player two in this frame.
 * @param createdAt The time the frame was recorded.
 */
public record FrameResponse(
    UUID id,
    int frameNumber,
    int playerOneBagsIn,
    int playerOneBagsOn,
    int playerTwoBagsIn,
    int playerTwoBagsOn,
    int playerOneFramePoints,
    int playerTwoFramePoints,
    Instant createdAt) {

}
