package com.github.solisa14.fourbagger.api.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.testsupport.AbstractDataJpaTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GameRepositoryTest extends AbstractDataJpaTest {

  @Autowired private GameRepository gameRepository;
  @Autowired private UserRepository userRepository;

  private User savedUser(String suffix) {
    return userRepository.saveAndFlush(
        TestDataFactory.user(
            null, "user" + suffix, "encoded", Role.USER));
  }

  @Test
  void findByPlayer_whenUserIsPlayerOneOrPlayerTwo_returnsMatchingGames() {
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
  void findByPlayer_whenUserIsPartnerInDoubles_returnsMatchingGames() {
    User p1 = savedUser("partner-a");
    User p1Partner = savedUser("partner-b");
    User p2 = savedUser("partner-c");
    User p2Partner = savedUser("partner-d");

    Game doublesGame =
        Game.builder()
            .playerOne(p1)
            .playerOnePartner(p1Partner)
            .playerTwo(p2)
            .playerTwoPartner(p2Partner)
            .gameType(GameType.DOUBLES)
            .status(GameStatus.IN_PROGRESS)
            .createdBy(p1)
            .build();

    gameRepository.saveAndFlush(doublesGame);

    assertThat(gameRepository.findByPlayer(p1Partner)).containsExactly(doublesGame);
    assertThat(gameRepository.findByPlayer(p2Partner)).containsExactly(doublesGame);
  }

  @Test
  void findByPlayer_whenUserHasNoGames_returnsEmptyList() {
    User p1 = savedUser("x");
    assertThat(gameRepository.findByPlayer(p1)).isEmpty();
  }

  @Test
  void saveAndFlush_whenGameHasFinalResult_persistsScoresWinnerAndSubmittedBy() {
    User p1 = savedUser("d");
    User p2 = savedUser("e");
    Instant completedAt = Instant.parse("2026-06-12T12:00:00Z");

    Game game = TestDataFactory.game(p1, p2, GameStatus.COMPLETED);
    game.setPlayerOneScore(21);
    game.setPlayerTwoScore(15);
    game.setWinner(p1);
    game.setSubmittedBy(p1);
    game.setCompletedAt(completedAt);

    Game saved = gameRepository.saveAndFlush(game);

    assertThat(saved.getPlayerOneScore()).isEqualTo(21);
    assertThat(saved.getPlayerTwoScore()).isEqualTo(15);
    assertThat(saved.getWinner().getId()).isEqualTo(p1.getId());
    assertThat(saved.getSubmittedBy().getId()).isEqualTo(p1.getId());
    assertThat(saved.getCompletedAt()).isEqualTo(completedAt);
  }
}
