package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.UUID;

/**
 * Command record containing the necessary information to create a new game.
 *
 * @param participants The participants (players and sides) involved in the game.
 * @param targetScore The target score required to win the game (optional, defaults to 21).
 * @param scoringMode The scoring mode for the game (optional, defaults to STANDARD).
 * @param tournamentMatchId The ID of the tournament match this game belongs to, if applicable.
 * @param createdBy The user who is creating the game.
 */
public record CreateGameCommand(
    GameParticipants participants,
    Integer targetScore,
    GameScoringMode scoringMode,
    UUID tournamentMatchId,
    User createdBy) {

  /**
   * Constructs a new {@link CreateGameCommand} and validates the required fields.
   *
   * @throws InvalidGameConfigurationException if participants or createdBy are null.
   */
  public CreateGameCommand {
    if (participants == null) {
      throw new InvalidGameConfigurationException("Game participants are required");
    }
    if (createdBy == null) {
      throw new InvalidGameConfigurationException("Game creator is required");
    }
  }

  /**
   * Gets the resolved target score, falling back to a default of 21 if not specified.
   *
   * @return The target score for the game.
   */
  public int resolvedTargetScore() {
    return targetScore != null ? targetScore : 21;
  }

  /**
   * Gets the resolved scoring mode, falling back to {@link GameScoringMode#STANDARD} if not
   * specified.
   *
   * @return The scoring mode for the game.
   */
  public GameScoringMode resolvedScoringMode() {
    return scoringMode != null ? scoringMode : GameScoringMode.STANDARD;
  }
}
