package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.UUID;

/** Command record containing the necessary information to create a new game. */
public record CreateGameCommand(
    GameParticipants participants, UUID tournamentMatchId, User createdBy) {

  public CreateGameCommand {
    if (participants == null) {
      throw new InvalidGameConfigurationException("Game participants are required");
    }
    if (createdBy == null) {
      throw new InvalidGameConfigurationException("Game creator is required");
    }
  }
}
