package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates the participants in a game, ensuring valid team configurations based on the game
 * type (singles vs doubles).
 *
 * @param gameType The type of game (singles or doubles).
 * @param teamOne The first team.
 * @param teamTwo The second team.
 */
public record GameParticipants(GameType gameType, GameSide teamOne, GameSide teamTwo) {

  /**
   * Constructs a {@link GameParticipants} record and validates the configuration. Ensures that all
   * participants are distinct and match the game type requirements.
   *
   * @throws InvalidGameConfigurationException if configuration is invalid or participants overlap.
   */
  public GameParticipants {
    if (gameType == null) {
      throw new InvalidGameConfigurationException("Game type is required");
    }
    if (teamOne == null || teamTwo == null) {
      throw new InvalidGameConfigurationException("Both teams are required");
    }
    if (gameType == GameType.SINGLES && (teamOne.partner() != null || teamTwo.partner() != null)) {
      throw new InvalidGameConfigurationException("Singles games cannot include partners");
    }
    if (gameType == GameType.DOUBLES && (teamOne.partner() == null || teamTwo.partner() == null)) {
      throw new InvalidGameConfigurationException("Doubles games require two players on each side");
    }
    validateUniqueParticipants(teamOne, teamTwo);
  }

  /**
   * Factory method to create participants for a singles game.
   *
   * @param playerOne First player.
   * @param playerTwo Second player.
   * @return A valid participants configuration for singles.
   */
  public static GameParticipants singles(User playerOne, User playerTwo) {
    return new GameParticipants(
        GameType.SINGLES, GameSide.singles(playerOne), GameSide.singles(playerTwo));
  }

  /**
   * Factory method to create participants for a doubles game.
   *
   * @param playerOne Player one on team one.
   * @param playerOnePartner Partner for player one on team one.
   * @param playerTwo Player two on team two.
   * @param playerTwoPartner Partner for player two on team two.
   * @return A valid participants configuration for doubles.
   */
  public static GameParticipants doubles(
      User playerOne, User playerOnePartner, User playerTwo, User playerTwoPartner) {
    return new GameParticipants(
        GameType.DOUBLES,
        GameSide.doubles(playerOne, playerOnePartner),
        GameSide.doubles(playerTwo, playerTwoPartner));
  }

  private static void validateUniqueParticipants(GameSide teamOne, GameSide teamTwo) {
    Set<UUID> seenIds = new HashSet<>();
    addParticipant(seenIds, teamOne.player());
    addParticipant(seenIds, teamTwo.player());
    if (teamOne.partner() != null) {
      addParticipant(seenIds, teamOne.partner());
    }
    if (teamTwo.partner() != null) {
      addParticipant(seenIds, teamTwo.partner());
    }
  }

  private static void addParticipant(Set<UUID> seenIds, User participant) {
    if (participant.getId() == null) {
      throw new InvalidGameConfigurationException("Game participants must have persisted IDs");
    }
    if (!seenIds.add(participant.getId())) {
      throw new InvalidGameConfigurationException("A player cannot appear on both sides of a game");
    }
  }
}
