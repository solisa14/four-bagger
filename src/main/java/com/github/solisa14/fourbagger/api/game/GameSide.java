package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.Objects;

public record GameSide(User player, User partner) {

  public GameSide {
    if (player == null) {
      throw new InvalidGameConfigurationException("A game side must include a primary player");
    }
  }

  public static GameSide singles(User player) {
    return new GameSide(player, null);
  }

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
