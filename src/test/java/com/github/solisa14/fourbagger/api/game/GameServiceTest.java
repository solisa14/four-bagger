package com.github.solisa14.fourbagger.api.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

  @Mock private GameRepository gameRepository;
  @Mock private GameCreationService gameCreationService;
  @Mock private GameRequestMapper gameRequestMapper;

  @InjectMocks private GameService gameService;

  private User playerOne() {
    return TestDataFactory.user(UUID.randomUUID(), "p1", "p1@example.com", "encoded", Role.USER);
  }

  private User playerTwo() {
    return TestDataFactory.user(UUID.randomUUID(), "p2", "p2@example.com", "encoded", Role.USER);
  }

  private Game inProgressGame(User p1, User p2) {
    return Game.builder()
        .id(UUID.randomUUID())
        .playerOne(p1)
        .playerTwo(p2)
        .targetScore(21)
        .winByTwo(false)
        .status(GameStatus.IN_PROGRESS)
        .createdBy(p1)
        .build();
  }

  // --- createGame ---

  @Test
  void createGame_whenOptionalSettingsMissing_usesDefaultsAndPendingStatus() {
    User p1 = playerOne();
    User p2 = playerTwo();
    CreateGameRequest request = new CreateGameRequest(p2.getId(), null, null);
    CreateGameCommand command =
        new CreateGameCommand(GameParticipants.singles(p1, p2), null, null, null, null, p1);
    when(gameRequestMapper.toCreateCommand(p1, request, null)).thenReturn(command);
    Game created =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .gameType(GameType.SINGLES)
            .scoringMode(GameScoringMode.STANDARD)
            .targetScore(21)
            .winByTwo(false)
            .status(GameStatus.PENDING)
            .createdBy(p1)
            .build();
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class))).thenReturn(created);

    Game game = gameService.createGame(p1, request);

    assertThat(game.getStatus()).isEqualTo(GameStatus.PENDING);
    assertThat(game.getPlayerOne()).isEqualTo(p1);
    assertThat(game.getPlayerTwo()).isEqualTo(p2);
    assertThat(game.getGameType()).isEqualTo(GameType.SINGLES);
    assertThat(game.getTargetScore()).isEqualTo(21);
    assertThat(game.isWinByTwo()).isFalse();
    verify(gameRequestMapper).toCreateCommand(p1, request, null);
  }

  @Test
  void createGame_whenCustomSettingsProvided_appliesTargetScoreAndWinByTwo() {
    User p1 = playerOne();
    User p2 = playerTwo();
    CreateGameRequest request = new CreateGameRequest(p2.getId(), 15, true);
    CreateGameCommand mappedCommand =
        new CreateGameCommand(
            GameParticipants.singles(p1, p2), 15, true, GameScoringMode.STANDARD, null, p1);
    when(gameRequestMapper.toCreateCommand(p1, request, null)).thenReturn(mappedCommand);
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenAnswer(
            inv -> {
              CreateGameCommand command = inv.getArgument(0);
              return Game.builder()
                  .id(UUID.randomUUID())
                  .playerOne(command.participants().teamOne().player())
                  .playerTwo(command.participants().teamTwo().player())
                  .gameType(command.participants().gameType())
                  .scoringMode(command.resolvedScoringMode())
                  .targetScore(command.resolvedTargetScore())
                  .winByTwo(command.resolvedWinByTwo())
                  .status(GameStatus.PENDING)
                  .createdBy(command.createdBy())
                  .build();
            });

    Game game = gameService.createGame(p1, request);

    assertThat(game.getTargetScore()).isEqualTo(15);
    assertThat(game.isWinByTwo()).isTrue();
  }

  @Test
  void createGame_whenDoublesRequest_buildsDoublesParticipants() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User p1Partner =
        TestDataFactory.user(UUID.randomUUID(), "p1p", "p1p@example.com", "encoded", Role.USER);
    User p2Partner =
        TestDataFactory.user(UUID.randomUUID(), "p2p", "p2p@example.com", "encoded", Role.USER);
    CreateGameRequest request =
        new CreateGameRequest(
            p2.getId(), p1Partner.getId(), p2Partner.getId(), GameType.DOUBLES, 21, false);
    CreateGameCommand mappedCommand =
        new CreateGameCommand(
            GameParticipants.doubles(p1, p1Partner, p2, p2Partner),
            21,
            false,
            GameScoringMode.STANDARD,
            null,
            p1);
    when(gameRequestMapper.toCreateCommand(p1, request, null)).thenReturn(mappedCommand);
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenAnswer(
            inv -> {
              CreateGameCommand command = inv.getArgument(0);
              return Game.builder()
                  .id(UUID.randomUUID())
                  .playerOne(command.participants().teamOne().player())
                  .playerOnePartner(command.participants().teamOne().partner())
                  .playerTwo(command.participants().teamTwo().player())
                  .playerTwoPartner(command.participants().teamTwo().partner())
                  .gameType(command.participants().gameType())
                  .scoringMode(command.resolvedScoringMode())
                  .targetScore(command.resolvedTargetScore())
                  .winByTwo(command.resolvedWinByTwo())
                  .status(GameStatus.PENDING)
                  .createdBy(command.createdBy())
                  .build();
            });

    Game game = gameService.createGame(p1, request);

    assertThat(game.getGameType()).isEqualTo(GameType.DOUBLES);
    assertThat(game.getPlayerOnePartner()).isEqualTo(p1Partner);
    assertThat(game.getPlayerTwoPartner()).isEqualTo(p2Partner);
  }

  @Test
  void createGame_whenDoublesPartnersMissing_throwsInvalidGameConfigurationException() {
    User p1 = playerOne();
    User p2 = playerTwo();

    CreateGameRequest request =
        new CreateGameRequest(p2.getId(), null, null, GameType.DOUBLES, 21, false);
    when(gameRequestMapper.toCreateCommand(p1, request, null))
        .thenThrow(new InvalidGameConfigurationException("Doubles games require both partner IDs"));

    assertThatThrownBy(() -> gameService.createGame(p1, request))
        .isInstanceOf(InvalidGameConfigurationException.class);
    verify(gameCreationService, never()).createPendingGame(any(CreateGameCommand.class));
  }

  @Test
  void createGame_withCommand_delegatesToCreationService() {
    User p1 = playerOne();
    User p2 = playerTwo();
    CreateGameCommand command =
        new CreateGameCommand(
            GameParticipants.singles(p1, p2), null, null, null, UUID.randomUUID(), p1);
    Game created = Game.builder().id(UUID.randomUUID()).playerOne(p1).playerTwo(p2).build();
    when(gameCreationService.createPendingGame(command)).thenReturn(created);

    Game result = gameService.createGame(command);

    assertThat(result).isEqualTo(created);
    verify(gameCreationService).createPendingGame(command);
  }

  // --- startGame ---

  @Test
  void startGame_whenGameIsPending_setsStatusToInProgress() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .status(GameStatus.PENDING)
            .createdBy(p1)
            .build();
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    Game started = gameService.startGame(game.getId());

    assertThat(started.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
  }

  @Test
  void startGame_whenGameIsNotPending_throwsInvalidGameStateException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.startGame(game.getId()))
        .isInstanceOf(InvalidGameStateException.class);
  }

  // --- recordFrame: cancellation scoring ---

  @Test
  void recordFrame_whenPlayerOneRawScoreIsHigher_awardsNetPointsToPlayerOne() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    // p1: 1 in (3pts) + 1 on (1pt) = 4; p2: 0 in + 1 on = 1; net = 3 to p1
    RecordFrameRequest request = new RecordFrameRequest(1, 1, 0, 1);
    Frame frame = gameService.recordFrame(game.getId(), request);

    assertThat(frame.getPlayerOneFramePoints()).isEqualTo(3);
    assertThat(frame.getPlayerTwoFramePoints()).isEqualTo(0);
    assertThat(game.getPlayerOneScore()).isEqualTo(3);
    assertThat(game.getPlayerTwoScore()).isEqualTo(0);
  }

  @Test
  void recordFrame_whenPlayerTwoRawScoreIsHigher_awardsNetPointsToPlayerTwo() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    // p1: 0 in + 0 on = 0; p2: 1 in (3pts) + 2 on (2pts) = 5; net = 5 to p2
    RecordFrameRequest request = new RecordFrameRequest(0, 0, 1, 2);
    Frame frame = gameService.recordFrame(game.getId(), request);

    assertThat(frame.getPlayerOneFramePoints()).isEqualTo(0);
    assertThat(frame.getPlayerTwoFramePoints()).isEqualTo(5);
    assertThat(game.getPlayerOneScore()).isEqualTo(0);
    assertThat(game.getPlayerTwoScore()).isEqualTo(5);
  }

  @Test
  void recordFrame_whenRawScoresAreEqual_awardsNoPoints() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    // p1: 1 in (3) = 3; p2: 1 in (3) = 3; cancels out
    RecordFrameRequest request = new RecordFrameRequest(1, 0, 1, 0);
    Frame frame = gameService.recordFrame(game.getId(), request);

    assertThat(frame.getPlayerOneFramePoints()).isEqualTo(0);
    assertThat(frame.getPlayerTwoFramePoints()).isEqualTo(0);
    assertThat(game.getPlayerOneScore()).isEqualTo(0);
    assertThat(game.getPlayerTwoScore()).isEqualTo(0);
  }

  @Test
  void recordFrame_whenPlayerOneHitsFourBagger_awardsTwelveNetPointsMinusOpponent() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    // p1: 4 in = 12pts; p2: 0; net = 12
    RecordFrameRequest request = new RecordFrameRequest(4, 0, 0, 0);
    Frame frame = gameService.recordFrame(game.getId(), request);

    assertThat(frame.getPlayerOneFramePoints()).isEqualTo(12);
    assertThat(game.getPlayerOneScore()).isEqualTo(12);
  }

  @Test
  void recordFrame_whenMultipleFramesRecorded_accumulatesScoresAcrossFrames() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    // Frame 1: p1 nets 3
    gameService.recordFrame(game.getId(), new RecordFrameRequest(1, 0, 0, 0));
    // Frame 2: p1 nets 3 more
    gameService.recordFrame(game.getId(), new RecordFrameRequest(1, 0, 0, 0));

    assertThat(game.getPlayerOneScore()).isEqualTo(6);
    assertThat(game.getFrames()).hasSize(2);
    assertThat(game.getFrames()).extracting(Frame::getFrameNumber).containsExactly(1, 2);
  }

  // --- win detection ---

  @Test
  void recordFrame_whenTargetReached_setsWinnerAndCompletesGame() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    game.setPlayerOneScore(18);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    // p1 nets 3 → total 21
    gameService.recordFrame(game.getId(), new RecordFrameRequest(1, 0, 0, 0));

    assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
    assertThat(game.getWinner()).isEqualTo(p1);
  }

  @Test
  void recordFrame_whenWinByTwoEnabledAndLeadIsOne_keepsGameInProgress() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .targetScore(21)
            .winByTwo(true)
            .status(GameStatus.IN_PROGRESS)
            .createdBy(p1)
            .playerOneScore(20)
            .playerTwoScore(20)
            .build();
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    // p1 nets 1 → 21 vs 20, lead = 1, not enough with winByTwo
    gameService.recordFrame(game.getId(), new RecordFrameRequest(0, 1, 0, 0));

    assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    assertThat(game.getWinner()).isNull();
  }

  @Test
  void recordFrame_whenWinByTwoEnabledAndLeadIsTwo_completesGame() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .targetScore(21)
            .winByTwo(true)
            .status(GameStatus.IN_PROGRESS)
            .createdBy(p1)
            .playerOneScore(21)
            .playerTwoScore(20)
            .build();
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    // p1 nets 1 more → 22 vs 20, lead = 2, game over
    gameService.recordFrame(game.getId(), new RecordFrameRequest(0, 1, 0, 0));

    assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
    assertThat(game.getWinner()).isEqualTo(p1);
  }

  @Test
  void recordFrame_whenExactModeAndScoreExceedsTarget_resetsScoringSideToEleven() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    game.setScoringMode(GameScoringMode.EXACT);
    game.setPlayerOneScore(20);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    gameService.recordFrame(game.getId(), new RecordFrameRequest(1, 0, 0, 0));

    assertThat(game.getPlayerOneScore()).isEqualTo(11);
    assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    assertThat(game.getWinner()).isNull();
  }

  @Test
  void recordFrame_whenExactModeAndTargetReachedExactly_completesGame() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    game.setScoringMode(GameScoringMode.EXACT);
    game.setPlayerOneScore(18);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    gameService.recordFrame(game.getId(), new RecordFrameRequest(1, 0, 0, 0));

    assertThat(game.getPlayerOneScore()).isEqualTo(21);
    assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
    assertThat(game.getWinner()).isEqualTo(p1);
  }

  // --- validation ---

  @Test
  void recordFrame_whenPlayerOneBagsExceedFour_throwsInvalidFrameException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(
            () -> gameService.recordFrame(game.getId(), new RecordFrameRequest(3, 2, 0, 0)))
        .isInstanceOf(InvalidFrameException.class);
  }

  @Test
  void recordFrame_whenPlayerTwoBagsExceedFour_throwsInvalidFrameException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(
            () -> gameService.recordFrame(game.getId(), new RecordFrameRequest(0, 0, 2, 3)))
        .isInstanceOf(InvalidFrameException.class);
  }

  @Test
  void recordFrame_whenGameIsPending_throwsInvalidGameStateException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .status(GameStatus.PENDING)
            .createdBy(p1)
            .build();
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(
            () -> gameService.recordFrame(game.getId(), new RecordFrameRequest(0, 0, 0, 0)))
        .isInstanceOf(InvalidGameStateException.class);
  }

  @Test
  void recordFrame_whenGameCompleted_throwsInvalidGameStateException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .status(GameStatus.COMPLETED)
            .createdBy(p1)
            .build();
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(
            () -> gameService.recordFrame(game.getId(), new RecordFrameRequest(0, 0, 0, 0)))
        .isInstanceOf(InvalidGameStateException.class);
  }

  // --- getGame ---

  @Test
  void getGame_whenGameNotFound_throwsGameNotFoundException() {
    UUID id = UUID.randomUUID();
    when(gameRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> gameService.getGame(id)).isInstanceOf(GameNotFoundException.class);
  }

  // --- cancelGame ---

  @Test
  void cancelGame_whenGameIsInProgress_setsStatusToCancelled() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    Game cancelled = gameService.cancelGame(game.getId());

    assertThat(cancelled.getStatus()).isEqualTo(GameStatus.CANCELLED);
  }

  @Test
  void cancelGame_whenGameAlreadyCompleted_throwsInvalidGameStateException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .status(GameStatus.COMPLETED)
            .createdBy(p1)
            .build();
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.cancelGame(game.getId()))
        .isInstanceOf(InvalidGameStateException.class);
  }

  // --- recordFrame: doubles ---

  @Test
  void recordFrame_whenDoublesAndThrowerIdsMissing_throwsInvalidFrameException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User p1Partner =
        TestDataFactory.user(UUID.randomUUID(), "p1p", "p1p@example.com", "encoded", Role.USER);
    User p2Partner =
        TestDataFactory.user(UUID.randomUUID(), "p2p", "p2p@example.com", "encoded", Role.USER);
    Game game = inProgressGame(p1, p2);
    game.setGameType(GameType.DOUBLES);
    game.setPlayerOnePartner(p1Partner);
    game.setPlayerTwoPartner(p2Partner);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(
            () -> gameService.recordFrame(game.getId(), new RecordFrameRequest(1, 0, 0, 0)))
        .isInstanceOf(InvalidFrameException.class);
  }

  @Test
  void recordFrame_whenDoublesAndWrongThrowersForOddFrame_throwsInvalidFrameException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User p1Partner =
        TestDataFactory.user(UUID.randomUUID(), "p1p", "p1p@example.com", "encoded", Role.USER);
    User p2Partner =
        TestDataFactory.user(UUID.randomUUID(), "p2p", "p2p@example.com", "encoded", Role.USER);
    Game game = inProgressGame(p1, p2);
    game.setGameType(GameType.DOUBLES);
    game.setPlayerOnePartner(p1Partner);
    game.setPlayerTwoPartner(p2Partner);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    RecordFrameRequest request =
        new RecordFrameRequest(1, 0, 0, 0, p1Partner.getId(), p2Partner.getId());

    assertThatThrownBy(() -> gameService.recordFrame(game.getId(), request))
        .isInstanceOf(InvalidFrameException.class);
  }

  @Test
  void recordFrame_whenDoublesAndCorrectThrowersForFrame_parsesSuccessfully() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User p1Partner =
        TestDataFactory.user(UUID.randomUUID(), "p1p", "p1p@example.com", "encoded", Role.USER);
    User p2Partner =
        TestDataFactory.user(UUID.randomUUID(), "p2p", "p2p@example.com", "encoded", Role.USER);
    Game game = inProgressGame(p1, p2);
    game.setGameType(GameType.DOUBLES);
    game.setPlayerOnePartner(p1Partner);
    game.setPlayerTwoPartner(p2Partner);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    Frame firstFrame =
        gameService.recordFrame(
            game.getId(), new RecordFrameRequest(1, 0, 0, 0, p1.getId(), p2.getId()));

    assertThat(firstFrame.getFrameNumber()).isEqualTo(1);
    assertThat(game.getFrames()).hasSize(1);

    Frame existing =
        Frame.builder()
            .game(game)
            .frameNumber(1)
            .playerOneBagsIn(1)
            .playerTwoBagsIn(0)
            .playerOneFramePoints(3)
            .playerTwoFramePoints(0)
            .build();
    game.getFrames().clear();
    game.getFrames().add(existing);

    Frame secondFrame =
        gameService.recordFrame(
            game.getId(), new RecordFrameRequest(0, 1, 0, 0, p1Partner.getId(), p2Partner.getId()));

    assertThat(secondFrame.getFrameNumber()).isEqualTo(2);
  }

  @Test
  void createGame_whenSinglesRequest_containsSinglesParticipantsInCommand() {
    User p1 = playerOne();
    User p2 = playerTwo();
    CreateGameRequest request = new CreateGameRequest(p2.getId(), 21, false);
    CreateGameCommand mappedCommand =
        new CreateGameCommand(
            GameParticipants.singles(p1, p2), 21, false, GameScoringMode.STANDARD, null, p1);
    when(gameRequestMapper.toCreateCommand(p1, request, null)).thenReturn(mappedCommand);
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenAnswer(
            inv -> {
              CreateGameCommand command = inv.getArgument(0);
              return Game.builder()
                  .id(UUID.randomUUID())
                  .playerOne(command.participants().teamOne().player())
                  .playerTwo(command.participants().teamTwo().player())
                  .gameType(command.participants().gameType())
                  .scoringMode(command.resolvedScoringMode())
                  .createdBy(command.createdBy())
                  .status(GameStatus.PENDING)
                  .build();
            });

    gameService.createGame(p1, request);

    ArgumentCaptor<CreateGameCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateGameCommand.class);
    verify(gameCreationService).createPendingGame(commandCaptor.capture());
    CreateGameCommand command = commandCaptor.getValue();
    assertThat(command.participants().gameType()).isEqualTo(GameType.SINGLES);
    assertThat(command.participants().teamOne().player()).isEqualTo(p1);
    assertThat(command.participants().teamTwo().player()).isEqualTo(p2);
  }
}
