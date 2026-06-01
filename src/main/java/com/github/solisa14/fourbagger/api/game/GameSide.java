package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.Objects;

/**
 * Represents one side (team) in a game, which can consist of a single player or two partners.
 *
 * @param player The primary player on this side.
 * @param partner The partner on this side (null for singles).
 */
public record GameSide(User player, User partner) {

  /**
   * Constructs a new {@link GameSide} and validates that a primary player is present.
   *
   * @throws InvalidGameConfigurationException if the primary player is null.
   */
  public GameSide {
    if (player == null) {
      throw new InvalidGameConfigurationException("A game side must include a primary player");
    }
  }

  /**
   * Creates a new singles side with only one player.
   *
   * @param player The primary player.
   * @return A new {@link GameSide} representing a singles team.
   */
  public static GameSide singles(User player) {
    return new GameSide(player, null);
  }

  /**
   * Creates a new doubles side with a primary player and a partner.
   *
   * @param player The primary player.
   * @param partner The partner player.
   * @return A new {@link GameSide} representing a doubles team.
   * @throws InvalidGameConfigurationException if partner is null or if player and partner are the
   *     same user.
   */
  public static GameSide doubles(User player, User partner) {
    if (partner == null) {
      throw new InvalidGameConfigurationException("Doubles side must include a partner");
    }
    if (Objects.equals(player.getId(), partner.getId())) {
      throw new InvalidGameConfigurationException(
          "Doubles side cannot contain the same player twice");
    }
    return new GameSide(player, partner);
  }
}
