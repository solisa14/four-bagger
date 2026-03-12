package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.UUID;

public record CreateGameCommand(
    GameParticipants participants,
    Integer targetScore,
    Boolean winByTwo,
    GameScoringMode scoringMode,
    UUID tournamentMatchId,
    User createdBy) {

  public CreateGameCommand {
    if (participants == null) {
      throw new InvalidGameConfigurationException("Game participants are required");
    }
    if (createdBy == null) {
      throw new InvalidGameConfigurationException("Game creator is required");
    }
  }

  public int resolvedTargetScore() {
    return targetScore != null ? targetScore : 21;
  }

  public boolean resolvedWinByTwo() {
    return winByTwo != null && winByTwo;
  }

  public GameScoringMode resolvedScoringMode() {
    return scoringMode != null ? scoringMode : GameScoringMode.STANDARD;
  }
}
