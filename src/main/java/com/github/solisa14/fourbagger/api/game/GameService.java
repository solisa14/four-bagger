package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core service containing business logic for game management. Handles creating games, recording
 * frames, and applying scoring rules.
 */
@Service
public class GameService {

  private final GameRepository gameRepository;
  private final GameCreationService gameCreationService;
  private final GameRequestMapper gameRequestMapper;
  private final Map<GameScoringMode, GameScoringPolicy> scoringPolicies =
      new EnumMap<>(GameScoringMode.class);

  public GameService(
      GameRepository gameRepository,
      GameCreationService gameCreationService,
      GameRequestMapper gameRequestMapper) {
    this.gameRepository = gameRepository;
    this.gameCreationService = gameCreationService;
    this.gameRequestMapper = gameRequestMapper;
    scoringPolicies.put(GameScoringMode.STANDARD, new StandardGameScoringPolicy());
    scoringPolicies.put(GameScoringMode.EXACT, new ExactGameScoringPolicy());
  }

  /**
   * Creates a new game from a user request.
   *
   * @param currentUser The user making the request.
   * @param request The request payload.
   * @return The created game.
   */
  @Transactional
  public Game createGame(User currentUser, CreateGameRequest request) {
    CreateGameCommand command = gameRequestMapper.toCreateCommand(currentUser, request, null);
    return gameCreationService.createPendingGame(command);
  }

  /**
   * Creates a new game from a domain command.
   *
   * @param command The command to create the game.
   * @return The created game.
   */
  @Transactional
  public Game createGame(CreateGameCommand command) {
    return gameCreationService.createPendingGame(command);
  }

  /**
   * Transitions a game from PENDING to IN_PROGRESS.
   *
   * @param gameId The ID of the game to start.
   * @return The updated game.
   * @throws InvalidGameStateException if the game is not PENDING.
   */
  @Transactional
  public Game startGame(User currentUser, UUID gameId) {
    Game game = getGame(gameId);
    authorizeMutation(currentUser, game);

    if (game.getStatus() != GameStatus.PENDING) {
      throw new InvalidGameStateException(
          "Cannot start a game that is not in PENDING status. Current status: " + game.getStatus());
    }

    game.setStatus(GameStatus.IN_PROGRESS);
    return gameRepository.save(game);
  }

  /**
   * Records a new frame for an in-progress game and applies scoring. In doubles, validates that the
   * throwers are alternating correctly.
   *
   * @param gameId The ID of the game.
   * @param request The frame details including bags in/on and thrower IDs.
   * @return The newly recorded frame.
   * @throws InvalidGameStateException if the game is not IN_PROGRESS.
   * @throws InvalidFrameException if bag counts are invalid or throwers are out of turn.
   */
  @Transactional
  public Frame recordFrame(User currentUser, UUID gameId, RecordFrameRequest request) {
    Game game = getGame(gameId);
    authorizeMutation(currentUser, game);

    if (game.getStatus() != GameStatus.IN_PROGRESS) {
      throw new InvalidGameStateException(
          "Cannot record a frame for a game that is not IN_PROGRESS. Current status: "
              + game.getStatus());
    }

    validateBagCounts(request);
    int frameNumber = game.getFrames().size() + 1;
    validateThrowersForFrame(game, frameNumber, request);

    int p1Points = 0;
    int p2Points = 0;
    int p1Raw = request.p1BagsIn() * 3 + request.p1BagsOn();
    int p2Raw = request.p2BagsIn() * 3 + request.p2BagsOn();
    if (p1Raw > p2Raw) {
      p1Points = p1Raw - p2Raw;
    } else if (p1Raw < p2Raw) {
      p2Points = p2Raw - p1Raw;
    }
    applyScoringPolicy(game, p1Points, p2Points);

    Frame frame =
        Frame.builder()
            .game(game)
            .frameNumber(frameNumber)
            .playerOneBagsIn(request.p1BagsIn())
            .playerOneBagsOn(request.p1BagsOn())
            .playerTwoBagsIn(request.p2BagsIn())
            .playerTwoBagsOn(request.p2BagsOn())
            .playerOneFramePoints(p1Points)
            .playerTwoFramePoints(p2Points)
            .build();

    game.getFrames().add(frame);
    gameRepository.save(game);
    return frame;
  }

  /**
   * Retrieves a game by its ID.
   *
   * @param gameId The ID of the game.
   * @return The game entity.
   * @throws GameNotFoundException if the game does not exist.
   */
  public Game getGame(UUID gameId) {
    return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
  }

  /**
   * Retrieves a list of games associated with a user.
   *
   * @param user The user.
   * @return List of games where the user is a player.
   */
  public List<Game> listUserGames(User user) {
    return gameRepository.findByPlayer(user);
  }

  /**
   * Cancels a game, preventing any further updates.
   *
   * @param gameId The ID of the game to cancel.
   * @return The cancelled game.
   * @throws InvalidGameStateException if the game is already completed or cancelled.
   */
  @Transactional
  public Game cancelGame(User currentUser, UUID gameId) {
    Game game = getGame(gameId);
    authorizeMutation(currentUser, game);

    if (game.getStatus() == GameStatus.COMPLETED || game.getStatus() == GameStatus.CANCELLED) {
      throw new InvalidGameStateException("Cannot cancel a game with status: " + game.getStatus());
    }

    game.setStatus(GameStatus.CANCELLED);
    return gameRepository.save(game);
  }

  private void validateBagCounts(RecordFrameRequest request) {
    if (request.p1BagsIn() + request.p1BagsOn() > 4) {
      throw new InvalidFrameException(
          "Player one's bags in ("
              + request.p1BagsIn()
              + ") + bags on ("
              + request.p1BagsOn()
              + ") cannot exceed 4");
    }
    if (request.p2BagsIn() + request.p2BagsOn() > 4) {
      throw new InvalidFrameException(
          "Player two's bags in ("
              + request.p2BagsIn()
              + ") + bags on ("
              + request.p2BagsOn()
              + ") cannot exceed 4");
    }
  }

  private void authorizeMutation(User currentUser, Game game) {
    UUID currentUserId = currentUser.getId();
    boolean isAuthorizedParticipant =
        game.getPlayerOne().getId().equals(currentUserId)
            || game.getPlayerTwo().getId().equals(currentUserId)
            || (game.getPlayerOnePartner() != null
                && game.getPlayerOnePartner().getId().equals(currentUserId))
            || (game.getPlayerTwoPartner() != null
                && game.getPlayerTwoPartner().getId().equals(currentUserId));

    boolean isGameCreator =
        game.getCreatedBy() != null && game.getCreatedBy().getId().equals(currentUserId);

    if (!isAuthorizedParticipant && !isGameCreator) {
      throw new GameAccessDeniedException(game.getId());
    }
  }

  private void applyScoringPolicy(Game game, int playerOneFramePoints, int playerTwoFramePoints) {
    GameScoringMode scoringMode =
        game.getScoringMode() != null ? game.getScoringMode() : GameScoringMode.STANDARD;
    GameScoringPolicy scoringPolicy = scoringPolicies.get(scoringMode);
    if (scoringPolicy == null) {
      throw new InvalidGameConfigurationException("Unsupported game scoring mode: " + scoringMode);
    }
    scoringPolicy.applyFrame(game, playerOneFramePoints, playerTwoFramePoints);
  }

  private void validateThrowersForFrame(Game game, int frameNumber, RecordFrameRequest request) {
    if (game.getGameType() != GameType.DOUBLES) {
      return;
    }

    if (game.getPlayerOnePartner() == null || game.getPlayerTwoPartner() == null) {
      throw new InvalidGameConfigurationException(
          "Doubles game is missing partner assignments and cannot record frames");
    }
    if (request.playerOneThrowerId() == null || request.playerTwoThrowerId() == null) {
      throw new InvalidFrameException("Doubles frames require both thrower IDs");
    }

    UUID expectedPlayerOneThrower =
        frameNumber % 2 == 1 ? game.getPlayerOne().getId() : game.getPlayerOnePartner().getId();
    UUID expectedPlayerTwoThrower =
        frameNumber % 2 == 1 ? game.getPlayerTwo().getId() : game.getPlayerTwoPartner().getId();

    if (!expectedPlayerOneThrower.equals(request.playerOneThrowerId())
        || !expectedPlayerTwoThrower.equals(request.playerTwoThrowerId())) {
      throw new InvalidFrameException(
          "Invalid throwing pair for frame "
              + frameNumber
              + "; doubles pairs must alternate by frame");
    }
  }
}
