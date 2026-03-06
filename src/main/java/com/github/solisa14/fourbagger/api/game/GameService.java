package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

  private final GameRepository gameRepository;
  private final UserService userService;

  public GameService(GameRepository gameRepository, UserService userService) {
    this.gameRepository = gameRepository;
    this.userService = userService;
  }

  @Transactional
  public Game createGame(User currentUser, CreateGameRequest request) {
    User playerTwo = userService.getUser(request.playerTwoId());

    Game game =
        Game.builder()
            .playerOne(currentUser)
            .playerTwo(playerTwo)
            .targetScore(request.resolvedTargetScore())
            .winByTwo(request.resolvedWinByTwo())
            .status(GameStatus.PENDING)
            .createdBy(currentUser)
            .build();

    return gameRepository.save(game);
  }

  @Transactional
  public Game startGame(UUID gameId) {
    Game game = getGame(gameId);

    if (game.getStatus() != GameStatus.PENDING) {
      throw new InvalidGameStateException(
          "Cannot start a game that is not in PENDING status. Current status: " + game.getStatus());
    }

    game.setStatus(GameStatus.IN_PROGRESS);
    return gameRepository.save(game);
  }

  @Transactional
  public Frame recordFrame(UUID gameId, RecordFrameRequest request) {
    Game game = getGame(gameId);

    if (game.getStatus() != GameStatus.IN_PROGRESS) {
      throw new InvalidGameStateException(
          "Cannot record a frame for a game that is not IN_PROGRESS. Current status: "
              + game.getStatus());
    }

    validateBagCounts(request);

    int p1Points = 0;
    int p2Points = 0;
    int p1Raw = request.p1BagsIn() * 3 + request.p1BagsOn();
    int p2Raw = request.p2BagsIn() * 3 + request.p2BagsOn();
    if (p1Raw > p2Raw) {
      p1Points = p1Raw - p2Raw;
    } else if (p1Raw < p2Raw) {
      p2Points = p2Raw - p1Raw;
    }
    game.setPlayerOneScore(game.getPlayerOneScore() + p1Points);
    game.setPlayerTwoScore(game.getPlayerTwoScore() + p2Points);
    checkAndSetWinner(game);

    Frame frame =
        Frame.builder()
            .game(game)
            .frameNumber(game.getFrames().size() + 1)
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

  public Game getGame(UUID gameId) {
    return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
  }

  public List<Game> listUserGames(User user) {
    return gameRepository.findByPlayer(user);
  }

  @Transactional
  public Game cancelGame(UUID gameId) {
    Game game = getGame(gameId);

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

  private void checkAndSetWinner(Game game) {
    int p1Score = game.getPlayerOneScore();
    int p2Score = game.getPlayerTwoScore();
    int target = game.getTargetScore();
    boolean winByTwo = game.isWinByTwo();

    boolean p1Wins = p1Score >= target && (!winByTwo || (p1Score - p2Score) >= 2);
    boolean p2Wins = p2Score >= target && (!winByTwo || (p2Score - p1Score) >= 2);

    if (p1Wins) {
      game.setWinner(game.getPlayerOne());
      game.setStatus(GameStatus.COMPLETED);
    } else if (p2Wins) {
      game.setWinner(game.getPlayerTwo());
      game.setStatus(GameStatus.COMPLETED);
    }
  }
}
