package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import com.github.solisa14.fourbagger.api.tournament.FinalScoreValidator;
import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Core service for standalone game lifecycle and final-result submission. */
@Service
public class GameService {

  private final GameRepository gameRepository;
  private final GameCreationService gameCreationService;
  private final FinalScoreValidator finalScoreValidator;

  public GameService(
      GameRepository gameRepository,
      GameCreationService gameCreationService,
      FinalScoreValidator finalScoreValidator) {
    this.gameRepository = gameRepository;
    this.gameCreationService = gameCreationService;
    this.finalScoreValidator = finalScoreValidator;
  }

  public Game createGame(CreateGameCommand command) {
    return gameCreationService.createPendingGame(command);
  }

  @Transactional
  public Game startGame(User currentUser, UUID gameId) {
    Game game = getGame(gameId);
    authorizeMutation(currentUser, game);

    if (game.getStatus() != GameStatus.PENDING) {
      throw new InvalidGameStateException(
          "Cannot start a game that is not in PENDING status. Current status: " + game.getStatus());
    }

    game.setStatus(GameStatus.IN_PROGRESS);
    Game savedGame = gameRepository.save(game);
    initializeGameDetails(savedGame);
    return savedGame;
  }

  @Transactional
  public Game submitResult(User currentUser, UUID gameId, SubmitGameResultRequest request) {
    Game game = getGame(gameId);
    authorizeMutation(currentUser, game);

    if (game.getTournamentMatchId() != null) {
      throw new InvalidGameStateException("Tournament games must use tournament result endpoints");
    }
    if (game.getCompletedAt() != null || game.getWinner() != null) {
      throw new ResultAlreadySubmittedException();
    }
    if (game.getStatus() != GameStatus.IN_PROGRESS) {
      throw new InvalidGameStateException(
          "Cannot submit a result for a game that is not IN_PROGRESS. Current status: "
              + game.getStatus());
    }

    validateWinnerInGame(game, request.winnerUserId());
    finalScoreValidator.validateScores(request.playerOneScore(), request.playerTwoScore());
    validateWinnerScore(game, request);

    game.setPlayerOneScore(request.playerOneScore());
    game.setPlayerTwoScore(request.playerTwoScore());
    game.setWinner(resolveWinnerUser(game, request.winnerUserId()));
    game.setSubmittedBy(currentUser);
    game.setCompletedAt(Instant.now());
    game.setStatus(GameStatus.COMPLETED);

    try {
      Game saved = gameRepository.saveAndFlush(game);
      initializeGameDetails(saved);
      return saved;
    } catch (ObjectOptimisticLockingFailureException ex) {
      throw new ResultAlreadySubmittedException();
    }
  }

  @Transactional(readOnly = true)
  public Game getGameForUser(User currentUser, UUID gameId) {
    Game game = getGame(gameId);
    if (!canAccessGame(currentUser, game)) {
      throw new GameAccessDeniedException();
    }
    initializeGameDetails(game);
    return game;
  }

  public Game getGame(UUID gameId) {
    return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
  }

  @Transactional(readOnly = true)
  public List<Game> listUserGames(User user) {
    List<Game> games = gameRepository.findByPlayer(user);
    games.forEach(this::initializeGameSummary);
    return games;
  }

  @Transactional
  public Game cancelGame(User currentUser, UUID gameId) {
    Game game = getGame(gameId);
    authorizeMutation(currentUser, game);

    if (game.getTournamentMatchId() != null) {
      throw new InvalidGameStateException("Tournament games cannot be cancelled");
    }

    if (game.getStatus() == GameStatus.COMPLETED || game.getStatus() == GameStatus.CANCELLED) {
      throw new InvalidGameStateException("Cannot cancel a game with status: " + game.getStatus());
    }

    game.setStatus(GameStatus.CANCELLED);
    Game savedGame = gameRepository.save(game);
    initializeGameDetails(savedGame);
    return savedGame;
  }

  private void initializeGameDetails(Game game) {
    initializeGameSummary(game);
  }

  private void initializeGameSummary(Game game) {
    game.getPlayerOne().getUsername();
    game.getPlayerTwo().getUsername();
    if (game.getPlayerOnePartner() != null) {
      game.getPlayerOnePartner().getUsername();
    }
    if (game.getPlayerTwoPartner() != null) {
      game.getPlayerTwoPartner().getUsername();
    }
    if (game.getWinner() != null) {
      game.getWinner().getUsername();
    }
    if (game.getSubmittedBy() != null) {
      game.getSubmittedBy().getUsername();
    }
  }

  private void validateWinnerInGame(Game game, UUID winnerUserId) {
    if (!isGameParticipant(game, winnerUserId)) {
      throw new BusinessException("Winner must be a game participant", HttpStatus.BAD_REQUEST);
    }
  }

  private void validateWinnerScore(Game game, SubmitGameResultRequest request) {
    boolean winnerIsPlayerOne =
        game.getPlayerOne().getId().equals(request.winnerUserId())
            || (game.getPlayerOnePartner() != null
                && game.getPlayerOnePartner().getId().equals(request.winnerUserId()));
    if (winnerIsPlayerOne && request.playerOneScore() <= request.playerTwoScore()) {
      throw new BusinessException("Winner must have the higher score", HttpStatus.BAD_REQUEST);
    }
    boolean winnerIsPlayerTwo =
        game.getPlayerTwo().getId().equals(request.winnerUserId())
            || (game.getPlayerTwoPartner() != null
                && game.getPlayerTwoPartner().getId().equals(request.winnerUserId()));
    if (winnerIsPlayerTwo && request.playerTwoScore() <= request.playerOneScore()) {
      throw new BusinessException("Winner must have the higher score", HttpStatus.BAD_REQUEST);
    }
  }

  private User resolveWinnerUser(Game game, UUID winnerUserId) {
    if (game.getPlayerOne().getId().equals(winnerUserId)) {
      return game.getPlayerOne();
    }
    if (game.getPlayerOnePartner() != null
        && game.getPlayerOnePartner().getId().equals(winnerUserId)) {
      return game.getPlayerOnePartner();
    }
    if (game.getPlayerTwo().getId().equals(winnerUserId)) {
      return game.getPlayerTwo();
    }
    return game.getPlayerTwoPartner();
  }

  private void authorizeMutation(User currentUser, Game game) {
    if (!canAccessGame(currentUser, game)) {
      throw new GameAccessDeniedException();
    }
  }

  private boolean canAccessGame(User currentUser, Game game) {
    return isGameParticipant(game, currentUser != null ? currentUser.getId() : null)
        || (currentUser != null && game.getCreatedBy().getId().equals(currentUser.getId()));
  }

  private boolean isGameParticipant(Game game, UUID userId) {
    if (userId == null) {
      return false;
    }
    return game.getPlayerOne().getId().equals(userId)
        || game.getPlayerTwo().getId().equals(userId)
        || (game.getPlayerOnePartner() != null
            && game.getPlayerOnePartner().getId().equals(userId))
        || (game.getPlayerTwoPartner() != null
            && game.getPlayerTwoPartner().getId().equals(userId));
  }
}
