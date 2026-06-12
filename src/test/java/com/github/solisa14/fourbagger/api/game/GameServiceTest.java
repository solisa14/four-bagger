package com.github.solisa14.fourbagger.api.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.tournament.FinalScoreValidator;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

  @Mock private GameRepository gameRepository;
  @Mock private GameCreationService gameCreationService;

  private final FinalScoreValidator finalScoreValidator = new FinalScoreValidator();
  private GameService gameService;

  @BeforeEach
  void setUp() {
    gameService = new GameService(gameRepository, gameCreationService, finalScoreValidator);
  }

  private User playerOne() {
    return TestDataFactory.user(UUID.randomUUID(), "p1", "encoded", Role.USER);
  }

  private User playerTwo() {
    return TestDataFactory.user(UUID.randomUUID(), "p2", "encoded", Role.USER);
  }

  private User otherUser() {
    return TestDataFactory.user(
        UUID.randomUUID(), "other", "encoded", Role.USER);
  }

  private Game inProgressGame(User p1, User p2) {
    return Game.builder()
        .id(UUID.randomUUID())
        .playerOne(p1)
        .playerTwo(p2)
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
        new CreateGameCommand(GameParticipants.singles(p1, p2), null, p1);
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

  // --- submitResult ---

  @Test
  void submitResult_whenValid_setsScoresWinnerAndCompletesGame() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.saveAndFlush(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p1.getId(), 21, 15);
    Game completed = gameService.submitResult(p1, game.getId(), request);

    assertThat(completed.getStatus()).isEqualTo(GameStatus.COMPLETED);
    assertThat(completed.getPlayerOneScore()).isEqualTo(21);
    assertThat(completed.getPlayerTwoScore()).isEqualTo(15);
    assertThat(completed.getWinner()).isEqualTo(p1);
    assertThat(completed.getSubmittedBy()).isEqualTo(p1);
    assertThat(completed.getCompletedAt()).isNotNull();
  }

  @Test
  void submitResult_whenPlayerTwoWins_setsPlayerTwoAsWinner() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.saveAndFlush(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p2.getId(), 18, 21);
    Game completed = gameService.submitResult(p1, game.getId(), request);

    assertThat(completed.getWinner()).isEqualTo(p2);
    assertThat(completed.getPlayerOneScore()).isEqualTo(18);
    assertThat(completed.getPlayerTwoScore()).isEqualTo(21);
  }

  @Test
  void submitResult_whenDoublesAndPartnerIsWinner_acceptsPartnerAsWinner() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User p1Partner =
        TestDataFactory.user(UUID.randomUUID(), "p1p", "encoded", Role.USER);
    User p2Partner =
        TestDataFactory.user(UUID.randomUUID(), "p2p", "encoded", Role.USER);
    Game game = inProgressGame(p1, p2);
    game.setGameType(GameType.DOUBLES);
    game.setPlayerOnePartner(p1Partner);
    game.setPlayerTwoPartner(p2Partner);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.saveAndFlush(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p2Partner.getId(), 10, 21);
    Game completed = gameService.submitResult(p1, game.getId(), request);

    assertThat(completed.getWinner()).isEqualTo(p2Partner);
  }

  @Test
  void submitResult_whenScoresAreTied_throwsBusinessException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p1.getId(), 21, 21);

    assertThatThrownBy(() -> gameService.submitResult(p1, game.getId(), request))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Scores cannot be tied");
  }

  @Test
  void submitResult_whenWinnerScoreNotHigher_throwsBusinessException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p1.getId(), 15, 21);

    assertThatThrownBy(() -> gameService.submitResult(p1, game.getId(), request))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Winner must have the higher score");
  }

  @Test
  void submitResult_whenWinnerNotParticipant_throwsBusinessException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User outsider = otherUser();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    SubmitGameResultRequest request = new SubmitGameResultRequest(outsider.getId(), 21, 15);

    assertThatThrownBy(() -> gameService.submitResult(p1, game.getId(), request))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Winner must be a game participant");
  }

  @Test
  void submitResult_whenGameIsPending_throwsInvalidGameStateException() {
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

    SubmitGameResultRequest request = new SubmitGameResultRequest(p1.getId(), 21, 15);

    assertThatThrownBy(() -> gameService.submitResult(p1, game.getId(), request))
        .isInstanceOf(InvalidGameStateException.class);
  }

  @Test
  void submitResult_whenResultAlreadySubmitted_throwsResultAlreadySubmittedException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    game.setWinner(p1);
    game.setCompletedAt(Instant.now());
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p1.getId(), 21, 15);

    assertThatThrownBy(() -> gameService.submitResult(p1, game.getId(), request))
        .isInstanceOf(ResultAlreadySubmittedException.class);
  }

  @Test
  void submitResult_whenGameIsCompleted_throwsResultAlreadySubmittedException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    game.setStatus(GameStatus.COMPLETED);
    game.setWinner(p1);
    game.setCompletedAt(Instant.now());
    game.setPlayerOneScore(21);
    game.setPlayerTwoScore(15);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p1.getId(), 21, 15);

    assertThatThrownBy(() -> gameService.submitResult(p1, game.getId(), request))
        .isInstanceOf(ResultAlreadySubmittedException.class)
        .isNotInstanceOf(InvalidGameStateException.class);
  }

  @Test
  void submitResult_whenTournamentGame_throwsInvalidGameStateException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    game.setTournamentMatchId(UUID.randomUUID());
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p1.getId(), 21, 15);

    assertThatThrownBy(() -> gameService.submitResult(p1, game.getId(), request))
        .isInstanceOf(InvalidGameStateException.class)
        .hasMessage("Tournament games must use tournament result endpoints");
  }

  @Test
  void submitResult_whenUserIsNotParticipantOrCreator_throwsGameAccessDeniedException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User outsider = otherUser();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p1.getId(), 21, 15);

    assertThatThrownBy(() -> gameService.submitResult(outsider, game.getId(), request))
        .isInstanceOf(GameAccessDeniedException.class);
  }

  @Test
  void submitResult_whenConcurrentSubmissionWins_throwsResultAlreadySubmittedException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(gameRepository.saveAndFlush(game))
        .thenThrow(new ObjectOptimisticLockingFailureException(Game.class, game.getId()));

    SubmitGameResultRequest request = new SubmitGameResultRequest(p1.getId(), 21, 15);

    assertThatThrownBy(() -> gameService.submitResult(p1, game.getId(), request))
        .isInstanceOf(ResultAlreadySubmittedException.class);
  }

  // --- getGame ---

  @Test
  void getGame_whenGameNotFound_throwsGameNotFoundException() {
    UUID id = UUID.randomUUID();
    when(gameRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> gameService.getGame(id)).isInstanceOf(GameNotFoundException.class);
  }

  @Test
  void getGameForUser_whenUserCanAccessGame_returnsGame() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    Game result = gameService.getGameForUser(p1, game.getId());

    assertThat(result).isEqualTo(game);
  }

  @Test
  void getGameForUser_whenUserIsNotParticipantOrCreator_throwsGameAccessDeniedException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    User outsider = otherUser();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.getGameForUser(outsider, game.getId()))
        .isInstanceOf(GameAccessDeniedException.class);
  }

  @Test
  void getGameForUser_whenUserIsMissing_throwsGameAccessDeniedException() {
    User p1 = playerOne();
    User p2 = playerTwo();
    Game game = inProgressGame(p1, p2);
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.getGameForUser(null, game.getId()))
        .isInstanceOf(GameAccessDeniedException.class);
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
}
