package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** REST controller for standalone game resources. */
@RestController
@RequestMapping("/api/v1/games")
public class GameController {

  private final GameService gameService;
  private final GameMapper gameMapper;

  public GameController(GameService gameService, GameMapper gameMapper) {
    this.gameService = gameService;
    this.gameMapper = gameMapper;
  }

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

  @PostMapping("/{gameId}/start")
  public ResponseEntity<GameResponse> startGame(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID gameId) {
    Game game = gameService.startGame(currentUser, gameId);
    return ResponseEntity.ok(gameMapper.toGameResponse(game));
  }

  @PostMapping("/{gameId}/result")
  public ResponseEntity<GameResponse> submitResult(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID gameId,
      @Valid @RequestBody SubmitGameResultRequest request) {
    Game game = gameService.submitResult(currentUser, gameId, request);
    return ResponseEntity.ok(gameMapper.toGameResponse(game));
  }

  @GetMapping("/{gameId}")
  public ResponseEntity<GameResponse> getGame(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID gameId) {
    Game game = gameService.getGameForUser(currentUser, gameId);
    return ResponseEntity.ok(gameMapper.toGameResponse(game));
  }

  @GetMapping("/me")
  public ResponseEntity<List<GameSummaryResponse>> listMyGames(
      @AuthenticationPrincipal User currentUser) {
    List<GameSummaryResponse> games =
        gameService.listUserGames(currentUser).stream()
            .map(gameMapper::toGameSummaryResponse)
            .toList();
    return ResponseEntity.ok(games);
  }

  @PostMapping("/{gameId}/cancel")
  public ResponseEntity<GameResponse> cancelGame(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID gameId) {
    Game game = gameService.cancelGame(currentUser, gameId);
    return ResponseEntity.ok(gameMapper.toGameResponse(game));
  }
}
