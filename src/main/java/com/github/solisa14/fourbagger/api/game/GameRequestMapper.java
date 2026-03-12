package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GameRequestMapper {

  private final UserService userService;

  public GameRequestMapper(UserService userService) {
    this.userService = userService;
  }

  public CreateGameCommand toCreateCommand(
      User currentUser, CreateGameRequest request, UUID tournamentMatchId) {
    User playerTwo = userService.getUser(request.playerTwoId());
    GameType gameType = resolveGameType(request);
    GameParticipants participants = resolveParticipants(currentUser, playerTwo, request, gameType);
    return new CreateGameCommand(
        participants,
        resolveTargetScore(request),
        resolveWinByTwo(request),
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

  private boolean resolveWinByTwo(CreateGameRequest request) {
    return request.winByTwo() != null && request.winByTwo();
  }

  private GameScoringMode resolveScoringMode(CreateGameRequest request) {
    return request.scoringMode() != null ? request.scoringMode() : GameScoringMode.STANDARD;
  }
}
