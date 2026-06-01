package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserService;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Mapper component responsible for transforming game-related API requests into internal domain
 * commands, and domain objects into responses.
 *
 * <p>Encapulates the logic for resolving participants and applying default game configuration
 * during the transformation process.
 */
@Component
public class GameMapper {

  private final UserService userService;

  /**
   * Constructs a new GameMapper.
   *
   * @param userService the user service for resolving participant details
   */
  public GameMapper(UserService userService) {
    this.userService = userService;
  }

  /**
   * Converts a {@link CreateGameRequest} into a {@link CreateGameCommand}. Resolves the
   * participants and applies default game rules where optional fields are not provided in the
   * request.
   *
   * @param currentUser The user initiating the request.
   * @param request The request payload containing game configuration.
   * @param tournamentMatchId The ID of the tournament match, if applicable.
   * @return A valid {@link CreateGameCommand}.
   */
  public CreateGameCommand toCreateCommand(
      User currentUser, CreateGameRequest request, UUID tournamentMatchId) {
    User playerTwo = userService.getUser(request.playerTwoId());
    GameType gameType = resolveGameType(request);
    GameParticipants participants = resolveParticipants(currentUser, playerTwo, request, gameType);
    return new CreateGameCommand(
        participants,
        resolveTargetScore(request),
        resolveScoringMode(request),
        tournamentMatchId,
        currentUser);
  }

  private GameParticipants resolveParticipants(
      User currentUser, User playerTwo, CreateGameRequest request, GameType gameType) {
    if (gameType == GameType.DOUBLES) {
      if (request.playerOnePartnerId() == null || request.playerTwoPartnerId() == null) {
        throw new InvalidGameConfigurationException(
            "Doubles games require both partner IDs to be provided");
      }
      User playerOnePartner = userService.getUser(request.playerOnePartnerId());
      User playerTwoPartner = userService.getUser(request.playerTwoPartnerId());
      return GameParticipants.doubles(currentUser, playerOnePartner, playerTwo, playerTwoPartner);
    }
    return GameParticipants.singles(currentUser, playerTwo);
  }

  private GameType resolveGameType(CreateGameRequest request) {
    if (request.gameType() != null) {
      return request.gameType();
    }
    return request.playerOnePartnerId() != null || request.playerTwoPartnerId() != null
        ? GameType.DOUBLES
        : GameType.SINGLES;
  }

  private int resolveTargetScore(CreateGameRequest request) {
    return request.targetScore() != null ? request.targetScore() : 21;
  }

  private GameScoringMode resolveScoringMode(CreateGameRequest request) {
    return request.scoringMode() != null ? request.scoringMode() : GameScoringMode.STANDARD;
  }

  /**
   * Converts a game entity to a detailed game response.
   *
   * @param game the game entity
   * @return the detailed game response
   */
  public GameResponse toGameResponse(Game game) {
    return new GameResponse(
        game.getId(),
        game.getGameType(),
        toPlayerInfo(game.getPlayerOne()),
        game.getPlayerOnePartner() != null ? toPlayerInfo(game.getPlayerOnePartner()) : null,
        toPlayerInfo(game.getPlayerTwo()),
        game.getPlayerTwoPartner() != null ? toPlayerInfo(game.getPlayerTwoPartner()) : null,
        game.getPlayerOneScore(),
        game.getPlayerTwoScore(),
        game.getTargetScore(),
        game.getStatus(),
        game.getWinner() != null ? toPlayerInfo(game.getWinner()) : null,
        game.getFrames().stream().map(this::toFrameResponse).toList(),
        game.getCreatedAt(),
        game.getUpdatedAt());
  }

  /**
   * Converts a game entity to a summary game response.
   *
   * @param game the game entity
   * @return the summary game response
   */
  public GameSummaryResponse toGameSummaryResponse(Game game) {
    return new GameSummaryResponse(
        game.getId(),
        game.getGameType(),
        toPlayerInfo(game.getPlayerOne()),
        game.getPlayerOnePartner() != null ? toPlayerInfo(game.getPlayerOnePartner()) : null,
        toPlayerInfo(game.getPlayerTwo()),
        game.getPlayerTwoPartner() != null ? toPlayerInfo(game.getPlayerTwoPartner()) : null,
        game.getPlayerOneScore(),
        game.getPlayerTwoScore(),
        game.getTargetScore(),
        game.getStatus(),
        game.getWinner() != null ? toPlayerInfo(game.getWinner()) : null,
        game.getCreatedAt(),
        game.getUpdatedAt());
  }

  /**
   * Converts a frame entity to a frame response.
   *
   * @param frame the frame entity
   * @return the frame response
   */
  public FrameResponse toFrameResponse(Frame frame) {
    return new FrameResponse(
        frame.getId(),
        frame.getFrameNumber(),
        frame.getPlayerOneBagsIn(),
        frame.getPlayerOneBagsOn(),
        frame.getPlayerTwoBagsIn(),
        frame.getPlayerTwoBagsOn(),
        frame.getPlayerOneFramePoints(),
        frame.getPlayerTwoFramePoints(),
        frame.getCreatedAt());
  }

  /**
   * Converts a user entity to a player info DTO.
   *
   * @param user the user entity
   * @return the player info DTO
   */
  public PlayerInfo toPlayerInfo(User user) {
    return new PlayerInfo(
        user.getId(), user.getUsername(), user.getFirstName(), user.getLastName());
  }
}
