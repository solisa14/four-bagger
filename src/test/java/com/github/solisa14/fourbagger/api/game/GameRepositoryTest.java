package com.github.solisa14.fourbagger.api.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.testsupport.AbstractDataJpaTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GameRepositoryTest extends AbstractDataJpaTest {

  @Autowired private GameRepository gameRepository;
  @Autowired private UserRepository userRepository;

  private User savedUser(String suffix) {
    return userRepository.saveAndFlush(
        TestDataFactory.user(
            null,
            "user" + suffix,
            "user" + suffix + "@example.com",
            "encoded",
            Role.USER));
  }

  @Test
  void findByPlayer_returnsGamesWhereUserIsPlayerOneOrTwo() {
    User p1 = savedUser("a");
    User p2 = savedUser("b");
    User other = savedUser("c");

    Game game1 = TestDataFactory.game(p1, p2, GameStatus.IN_PROGRESS);
    Game game2 = TestDataFactory.game(other, p1, GameStatus.PENDING);
    Game game3 = TestDataFactory.game(other, p2, GameStatus.PENDING);

    gameRepository.saveAndFlush(game1);
    gameRepository.saveAndFlush(game2);
    gameRepository.saveAndFlush(game3);

    List<Game> p1Games = gameRepository.findByPlayer(p1);
    List<Game> p2Games = gameRepository.findByPlayer(p2);

    assertThat(p1Games).hasSize(2);
    assertThat(p2Games).hasSize(2);
  }

  @Test
  void findByPlayer_returnsEmptyWhenUserHasNoGames() {
    User p1 = savedUser("x");
    assertThat(gameRepository.findByPlayer(p1)).isEmpty();
  }

  @Test
  void saveGame_cascadesPersistsFrames() {
    User p1 = savedUser("d");
    User p2 = savedUser("e");

    Game game = TestDataFactory.game(p1, p2, GameStatus.IN_PROGRESS);
    Frame frame =
        Frame.builder()
            .game(game)
            .frameNumber(1)
            .playerOneBagsIn(1)
            .playerOneFramePoints(3)
            .build();
    game.getFrames().add(frame);

    Game saved = gameRepository.saveAndFlush(game);

    assertThat(saved.getFrames()).hasSize(1);
    assertThat(saved.getFrames().get(0).getPlayerOneFramePoints()).isEqualTo(3);
  }
}
