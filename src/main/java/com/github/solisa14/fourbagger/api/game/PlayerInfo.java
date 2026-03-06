package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.UUID;

public record PlayerInfo(UUID id, String username, String firstName, String lastName) {

  public static PlayerInfo from(User user) {
    return new PlayerInfo(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName());
  }
}
