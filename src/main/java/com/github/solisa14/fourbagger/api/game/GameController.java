package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for managing game resources.
 *
 * <p>Provides endpoints for creating, starting, recording frames, and retrieving game details.
 */
@RestController
@RequestMapping("/api/v1/games")
public class GameController {

  private final GameService gameService;
  private final GameMapper gameMapper;

  /**
   * Constructs a new GameController.
   *
   * @param gameService the game service for business logic
   * @param gameMapper the game mapper for conversion between DTOs and domain objects
   */
  public GameController(GameService gameService, GameMapper gameMapper) {
    this.gameService = gameService;
    this.gameMapper = gameMapper;
  }

  /**
   * Creates a new game.
   *
   * @param currentUser The currently authenticated user creating the game.
   * @param request The request payload containing game configuration.
   * @return A response containing the newly created game details.
   */
  @PostMapping
  public ResponseEntity<GameResponse> createGame(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody CreateGameRequest request) {
    CreateGameCommand command = gameMapper.toCreateCommand(currentUser, request, null);
    Game game = gameService.createGame(command);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(game.getId())
            .toUri();
    return ResponseEntity.created(location).body(gameMapper.toGameResponse(game));
  }

  /**
   * Starts an existing pending game.
   *
   * @param currentUser the currently authenticated user
   * @param gameId The ID of the game to start.
   * @return A response containing the updated game details.
   */
  @PostMapping("/{gameId}/start")
  public ResponseEntity<GameResponse> startGame(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID gameId) {
    Game game = gameService.startGame(currentUser, gameId);
    return ResponseEntity.ok(gameMapper.toGameResponse(game));
  }

  /**
   * Records a new frame for an in-progress game.
   *
   * @param currentUser the currently authenticated user
   * @param gameId The ID of the game.
   * @param request The request payload containing frame details.
   * @return A response containing the updated game details.
   */
  @PostMapping("/{gameId}/frames")
  public ResponseEntity<GameResponse> recordFrame(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID gameId,
      @Valid @RequestBody RecordFrameRequest request) {
    gameService.recordFrame(currentUser, gameId, request);
    Game game = gameService.getGame(gameId);
    return ResponseEntity.status(201).body(gameMapper.toGameResponse(game));
  }

  /**
   * Retrieves the details of a specific game.
   *
   * @param gameId The ID of the game to retrieve.
   * @return A response containing the game details.
   */
  @GetMapping("/{gameId}")
  public ResponseEntity<GameResponse> getGame(@PathVariable UUID gameId) {
    Game game = gameService.getGame(gameId);
    return ResponseEntity.ok(gameMapper.toGameResponse(game));
  }

  /**
   * Lists the games played by the currently authenticated user.
   *
   * @param currentUser The currently authenticated user.
   * @return A response containing a list of summary views of the user's games.
   */
  @GetMapping("/me")
  public ResponseEntity<List<GameSummaryResponse>> listMyGames(
      @AuthenticationPrincipal User currentUser) {
    List<GameSummaryResponse> games =
        gameService.listUserGames(currentUser).stream()
            .map(gameMapper::toGameSummaryResponse)
            .toList();
    return ResponseEntity.ok(games);
  }

  /**
   * Cancels an existing game.
   *
   * @param currentUser the currently authenticated user
   * @param gameId The ID of the game to cancel.
   * @return A response containing the cancelled game details.
   */
  @PostMapping("/{gameId}/cancel")
  public ResponseEntity<GameResponse> cancelGame(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID gameId) {
    Game game = gameService.cancelGame(currentUser, gameId);
    return ResponseEntity.ok(gameMapper.toGameResponse(game));
  }
}
