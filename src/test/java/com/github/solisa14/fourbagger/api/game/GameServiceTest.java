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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

  @Mock private GameRepository gameRepository;
  @Mock private GameCreationService gameCreationService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private GameService gameService;

  private User playerOne() {
    return TestDataFactory.user(UUID.randomUUID(), "p1", "p1@example.com", "encoded", Role.USER);
  }

  private User playerTwo() {
    return TestDataFactory.user(UUID.randomUUID(), "p2", "p2@example.com", "encoded", Role.USER);
  }

  private User otherUser() {
    return TestDataFactory.user(
        UUID.randomUUID(), "other", "other@example.com", "encoded", Role.USER);
  }

  private Game inProgressGame(User p1, User p2) {
    return Game.builder()
        .id(UUID.randomUUID())
        .playerOne(p1)
        .playerTwo(p2)
        .targetScore(21)
        .status(GameStatus.IN_PROGRESS)
        .createdBy(p1)
        .build();
  }

  // --- createGame ---

  @Test
  void createGame_withCommand_delegatesToCreationService() {
    User p1 = playerOne();
    User p2 = playerTwo();
    CreateGameCommand command =
        new CreateGameCommand(GameParticipants.singles(p1, p2), null, null, UUID.randomUUID(), p1);
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

    Game started = gameService.startGame(p1, game.getId());

    assertThat(started.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
  }

  @Test
  void startGame_whenGameIsNotPending_throwsInvalidGameStateException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.startGame(p1, game.getId()))
        .isInstanceOf(InvalidGameStateException.class);
  }

  @Test
  void startGame_whenUserIsNotParticipantOrCreator_throwsGameAccessDeniedException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User outsider = otherUser();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .status(GameStatus.PENDING)
            .createdBy(p1)
            .build();
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.startGame(outsider, game.getId()))
        .isInstanceOf(GameAccessDeniedException.class);
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
    Frame frame = gameService.recordFrame(p1, game.getId(), request);

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
    Frame frame = gameService.recordFrame(p1, game.getId(), request);

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
    Frame frame = gameService.recordFrame(p1, game.getId(), request);

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
    Frame frame = gameService.recordFrame(p1, game.getId(), request);

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
    gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(1, 0, 0, 0));
    // Frame 2: p1 nets 3 more
    gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(1, 0, 0, 0));

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
    gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(1, 0, 0, 0));

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

    gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(1, 0, 0, 0));

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

    gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(1, 0, 0, 0));

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
            () -> gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(3, 2, 0, 0)))
        .isInstanceOf(InvalidFrameException.class);
  }

  @Test
  void recordFrame_whenPlayerTwoBagsExceedFour_throwsInvalidFrameException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(
            () -> gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(0, 0, 2, 3)))
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
            () -> gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(0, 0, 0, 0)))
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
            () -> gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(0, 0, 0, 0)))
        .isInstanceOf(InvalidGameStateException.class);
  }

  @Test
  void recordFrame_whenUserIsNotParticipantOrCreator_throwsGameAccessDeniedException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User outsider = otherUser();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(
            () ->
                gameService.recordFrame(outsider, game.getId(), new RecordFrameRequest(1, 0, 0, 0)))
        .isInstanceOf(GameAccessDeniedException.class);
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

    Game cancelled = gameService.cancelGame(p1, game.getId());

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

    assertThatThrownBy(() -> gameService.cancelGame(p1, game.getId()))
        .isInstanceOf(InvalidGameStateException.class);
  }

  @Test
  void cancelGame_whenGameBelongsToTournament_throwsInvalidGameStateException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    game.setTournamentMatchId(UUID.randomUUID());
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.cancelGame(p1, game.getId()))
        .isInstanceOf(InvalidGameStateException.class)
        .hasMessage("Tournament games cannot be cancelled");
  }

  @Test
  void cancelGame_whenUserIsNotParticipantOrCreator_throwsGameAccessDeniedException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User outsider = otherUser();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.cancelGame(outsider, game.getId()))
        .isInstanceOf(GameAccessDeniedException.class);
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
            () -> gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(1, 0, 0, 0)))
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

    assertThatThrownBy(() -> gameService.recordFrame(p1, game.getId(), request))
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
            p1, game.getId(), new RecordFrameRequest(1, 0, 0, 0, p1.getId(), p2.getId()));

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
            p1,
            game.getId(),
            new RecordFrameRequest(0, 1, 0, 0, p1Partner.getId(), p2Partner.getId()));

    assertThat(secondFrame.getFrameNumber()).isEqualTo(2);
  }

  // --- event publishing ---

  @Test
  void recordFrame_whenScoringCompletesGame_publishesGameCompletedEvent() {
    User p1 = playerOne();
    User p2 = playerTwo();
    UUID matchId = UUID.randomUUID();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .targetScore(21)
            .playerOneScore(18)
            .status(GameStatus.IN_PROGRESS)
            .createdBy(p1)
            .tournamentMatchId(matchId)
            .build();
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any())).thenReturn(game);

    gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(1, 0, 0, 0));

    ArgumentCaptor<GameCompletedEvent> captor = ArgumentCaptor.forClass(GameCompletedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().gameId()).isEqualTo(game.getId());
    assertThat(captor.getValue().tournamentMatchId()).isEqualTo(matchId);
  }

  @Test
  void recordFrame_whenFrameDoesNotCompleteGame_doesNotPublishEvent() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .playerOne(p1)
            .playerTwo(p2)
            .targetScore(21)
            .playerOneScore(0)
            .status(GameStatus.IN_PROGRESS)
            .createdBy(p1)
            .tournamentMatchId(UUID.randomUUID())
            .build();
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.save(any())).thenReturn(game);

    gameService.recordFrame(p1, game.getId(), new RecordFrameRequest(1, 0, 0, 0));

    verify(eventPublisher, never()).publishEvent(any());
  }
}
