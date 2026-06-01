package com.github.solisa14.fourbagger.api.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload for recording a frame in an active game. Contains the number of bags in the hole
 * and on the board for each side, as well as the IDs of the players who threw the bags (required
 * for doubles).
 *
 * @param p1BagsIn Number of bags team one threw into the hole (0-4).
 * @param p1BagsOn Number of bags team one threw onto the board (0-4).
 * @param p2BagsIn Number of bags team two threw into the hole (0-4).
 * @param p2BagsOn Number of bags team two threw onto the board (0-4).
 * @param playerOneThrowerId The ID of the player throwing for team one.
 * @param playerTwoThrowerId The ID of the player throwing for team two.
 */
public record RecordFrameRequest(
    @NotNull @Min(0) @Max(4) Integer p1BagsIn,
    @NotNull @Min(0) @Max(4) Integer p1BagsOn,
    @NotNull @Min(0) @Max(4) Integer p2BagsIn,
    @NotNull @Min(0) @Max(4) Integer p2BagsOn,
    UUID playerOneThrowerId,
    UUID playerTwoThrowerId) {

  /**
   * Constructor intended for singles games where thrower IDs are implicitly the primary players.
   *
   * @param p1BagsIn Number of bags team one threw into the hole.
   * @param p1BagsOn Number of bags team one threw onto the board.
   * @param p2BagsIn Number of bags team two threw into the hole.
   * @param p2BagsOn Number of bags team two threw onto the board.
   */
  public RecordFrameRequest(
      Integer p1BagsIn, Integer p1BagsOn, Integer p2BagsIn, Integer p2BagsOn) {
    this(p1BagsIn, p1BagsOn, p2BagsIn, p2BagsOn, null, null);
  }
}
