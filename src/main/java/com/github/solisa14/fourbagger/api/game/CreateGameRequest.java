package com.github.solisa14.fourbagger.api.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload for creating a new game.
 *
 * @param playerTwoId The ID of the opponent (player two).
 * @param playerOnePartnerId The ID of player one's partner (for doubles).
 * @param playerTwoPartnerId The ID of player two's partner (for doubles).
 * @param gameType The type of game (SINGLES or DOUBLES).
 * @param scoringMode The scoring mode (STANDARD or EXACT).
 * @param targetScore The target score to win the game (must be between 11 and 21).
 */
public record CreateGameRequest(
    @NotNull UUID playerTwoId,
    UUID playerOnePartnerId,
    UUID playerTwoPartnerId,
    GameType gameType,
    GameScoringMode scoringMode,
    @Min(11) @Max(21) Integer targetScore) {

  /**
   * Constructor for creating a game without explicitly specifying the scoring mode.
   *
   * @param playerTwoId The ID of the opponent.
   * @param playerOnePartnerId The ID of player one's partner.
   * @param playerTwoPartnerId The ID of player two's partner.
   * @param gameType The type of game.
   * @param targetScore The target score to win.
   */
  public CreateGameRequest(
      UUID playerTwoId,
      UUID playerOnePartnerId,
      UUID playerTwoPartnerId,
      GameType gameType,
      Integer targetScore) {
    this(playerTwoId, playerOnePartnerId, playerTwoPartnerId, gameType, null, targetScore);
  }

  /**
   * Convenience constructor for creating a basic singles game.
   *
   * @param playerTwoId The ID of the opponent.
   * @param targetScore The target score to win.
   */
  public CreateGameRequest(UUID playerTwoId, Integer targetScore) {
    this(playerTwoId, null, null, null, null, targetScore);
  }
}
