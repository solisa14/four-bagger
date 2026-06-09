package com.github.solisa14.fourbagger.api.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GameEncapsulationTest {

  @Test
  void framesRejectExternalMutationAndSynchronizeOwningGame() {
    User playerOne = user("one");
    Game game = Game.restore(playerOne, user("two"), GameStatus.IN_PROGRESS, playerOne);
    Frame frame = Frame.builder().frameNumber(1).build();

    game.addFrame(frame);

    assertThat(frame.getGame()).isSameAs(game);
    assertThatThrownBy(() -> game.getFrames().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void frameCannotBeMovedToAnotherGame() {
    User playerOne = user("one");
    Game first = Game.restore(playerOne, user("two"), GameStatus.IN_PROGRESS, playerOne);
    Game second = Game.restore(playerOne, user("three"), GameStatus.IN_PROGRESS, playerOne);
    Frame frame = Frame.builder().frameNumber(1).build();
    first.addFrame(frame);

    assertThatThrownBy(() -> second.addFrame(frame))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("another game");
  }

  private User user(String suffix) {
    return TestDataFactory.user(
        UUID.randomUUID(), suffix, suffix + "@example.com", "encoded", Role.USER);
  }
}
