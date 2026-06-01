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
  void toCreateCommand_whenNoOptionalValues_appliesDefaultsInMapper() {
    User currentUser = user("creator");
    User playerTwo = user("opponent");
    CreateGameRequest request = new CreateGameRequest(playerTwo.getId(), null);
    when(userService.getUser(playerTwo.getId())).thenReturn(playerTwo);

    CreateGameCommand command = gameMapper.toCreateCommand(currentUser, request, null);

    assertThat(command.participants().gameType()).isEqualTo(GameType.SINGLES);
    assertThat(command.participants().teamOne().player()).isEqualTo(currentUser);
    assertThat(command.participants().teamTwo().player()).isEqualTo(playerTwo);
    assertThat(command.resolvedTargetScore()).isEqualTo(21);
    assertThat(command.resolvedScoringMode()).isEqualTo(GameScoringMode.STANDARD);
  }

  @Test
  void toCreateCommand_whenPartnersProvidedAndTypeMissing_infersDoubles() {
    User currentUser = user("creator");
    User playerTwo = user("opponent");
    User playerOnePartner = user("team1b");
    User playerTwoPartner = user("team2b");
    CreateGameRequest request =
        new CreateGameRequest(
            playerTwo.getId(),
            playerOnePartner.getId(),
            playerTwoPartner.getId(),
            null,
            GameScoringMode.EXACT,
            21);
    when(userService.getUser(playerTwo.getId())).thenReturn(playerTwo);
    when(userService.getUser(playerOnePartner.getId())).thenReturn(playerOnePartner);
    when(userService.getUser(playerTwoPartner.getId())).thenReturn(playerTwoPartner);

    CreateGameCommand command = gameMapper.toCreateCommand(currentUser, request, null);

    assertThat(command.participants().gameType()).isEqualTo(GameType.DOUBLES);
    assertThat(command.participants().teamOne().partner()).isEqualTo(playerOnePartner);
    assertThat(command.participants().teamTwo().partner()).isEqualTo(playerTwoPartner);
    assertThat(command.resolvedScoringMode()).isEqualTo(GameScoringMode.EXACT);
  }

  @Test
  void toCreateCommand_whenDoublesAndPartnerMissing_throwsInvalidGameConfigurationException() {
    User currentUser = user("creator");
    User playerTwo = user("opponent");
    CreateGameRequest request =
        new CreateGameRequest(playerTwo.getId(), null, null, GameType.DOUBLES, null, 21);
    when(userService.getUser(playerTwo.getId())).thenReturn(playerTwo);

    assertThatThrownBy(() -> gameMapper.toCreateCommand(currentUser, request, null))
        .isInstanceOf(InvalidGameConfigurationException.class);
  }

  private User user(String suffix) {
    return TestDataFactory.user(
        UUID.randomUUID(), suffix, suffix + "@example.com", "encoded", Role.USER);
  }
}
