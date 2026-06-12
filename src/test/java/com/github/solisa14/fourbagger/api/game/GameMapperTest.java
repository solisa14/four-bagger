package com.github.solisa14.fourbagger.api.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameMapperTest {

  @Mock private UserService userService;

  @InjectMocks private GameMapper gameMapper;

  @Test
  void toCreateCommand_whenNoOptionalValues_appliesSinglesDefaults() {
    User currentUser = user("creator");
    User playerTwo = user("opponent");
    CreateGameRequest request = new CreateGameRequest(playerTwo.getId());
    when(userService.getUser(playerTwo.getId())).thenReturn(playerTwo);

    CreateGameCommand command = gameMapper.toCreateCommand(currentUser, request, null);

    assertThat(command.participants().gameType()).isEqualTo(GameType.SINGLES);
    assertThat(command.participants().teamOne().player()).isEqualTo(currentUser);
    assertThat(command.participants().teamTwo().player()).isEqualTo(playerTwo);
    assertThat(command.tournamentMatchId()).isNull();
    assertThat(command.createdBy()).isEqualTo(currentUser);
  }

  @Test
  void toCreateCommand_whenPartnersProvidedAndTypeMissing_infersDoubles() {
    User currentUser = user("creator");
    User playerTwo = user("opponent");
    User playerOnePartner = user("team1b");
    User playerTwoPartner = user("team2b");
    CreateGameRequest request =
        new CreateGameRequest(
            playerTwo.getId(), playerOnePartner.getId(), playerTwoPartner.getId(), null);
    when(userService.getUser(playerTwo.getId())).thenReturn(playerTwo);
    when(userService.getUser(playerOnePartner.getId())).thenReturn(playerOnePartner);
    when(userService.getUser(playerTwoPartner.getId())).thenReturn(playerTwoPartner);

    CreateGameCommand command = gameMapper.toCreateCommand(currentUser, request, null);

    assertThat(command.participants().gameType()).isEqualTo(GameType.DOUBLES);
    assertThat(command.participants().teamOne().partner()).isEqualTo(playerOnePartner);
    assertThat(command.participants().teamTwo().partner()).isEqualTo(playerTwoPartner);
  }

  @Test
  void toCreateCommand_whenDoublesAndPartnerMissing_throwsInvalidGameConfigurationException() {
    User currentUser = user("creator");
    User playerTwo = user("opponent");
    CreateGameRequest request =
        new CreateGameRequest(playerTwo.getId(), null, null, GameType.DOUBLES);
    when(userService.getUser(playerTwo.getId())).thenReturn(playerTwo);

    assertThatThrownBy(() -> gameMapper.toCreateCommand(currentUser, request, null))
        .isInstanceOf(InvalidGameConfigurationException.class);
  }

  @Test
  void toGameResponse_whenGameCompleted_includesSubmittedByAndCompletedAt() {
    User playerOne = user("p1");
    User playerTwo = user("p2");
    java.time.Instant completedAt = java.time.Instant.parse("2026-06-12T12:00:00Z");
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(playerOne)
            .playerTwo(playerTwo)
            .playerOneScore(21)
            .playerTwoScore(15)
            .status(GameStatus.COMPLETED)
            .winner(playerOne)
            .submittedBy(playerOne)
            .completedAt(completedAt)
            .createdBy(playerOne)
            .build();

    GameResponse response = gameMapper.toGameResponse(game);

    assertThat(response.playerOneScore()).isEqualTo(21);
    assertThat(response.playerTwoScore()).isEqualTo(15);
    assertThat(response.winner().id()).isEqualTo(playerOne.getId());
    assertThat(response.submittedBy().id()).isEqualTo(playerOne.getId());
    assertThat(response.completedAt()).isEqualTo(completedAt);
  }

  @Test
  void toGameSummaryResponse_excludesSubmittedBy() {
    User playerOne = user("p1");
    User playerTwo = user("p2");
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(playerOne)
            .playerTwo(playerTwo)
            .status(GameStatus.PENDING)
            .createdBy(playerOne)
            .build();

    GameSummaryResponse response = gameMapper.toGameSummaryResponse(game);

    assertThat(response.id()).isEqualTo(game.getId());
    assertThat(response.playerOne().id()).isEqualTo(playerOne.getId());
    assertThat(response.status()).isEqualTo(GameStatus.PENDING);
  }

  private User user(String suffix) {
    return TestDataFactory.user(
        UUID.randomUUID(), suffix, "encoded", Role.USER);
  }
}
