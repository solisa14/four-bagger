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

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

  private final GameService gameService;

  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  @PostMapping
  public ResponseEntity<GameResponse> createGame(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody CreateGameRequest request) {
    Game game = gameService.createGame(currentUser, request);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(game.getId())
            .toUri();
    return ResponseEntity.created(location).body(GameResponse.from(game));
  }

  @PostMapping("/{gameId}/start")
  public ResponseEntity<GameResponse> startGame(@PathVariable UUID gameId) {
    Game game = gameService.startGame(gameId);
    return ResponseEntity.ok(GameResponse.from(game));
  }

  @PostMapping("/{gameId}/frames")
  public ResponseEntity<GameResponse> recordFrame(
      @PathVariable UUID gameId, @Valid @RequestBody RecordFrameRequest request) {
    gameService.recordFrame(gameId, request);
    Game game = gameService.getGame(gameId);
    return ResponseEntity.status(201).body(GameResponse.from(game));
  }

  @GetMapping("/{gameId}")
  public ResponseEntity<GameResponse> getGame(@PathVariable UUID gameId) {
    Game game = gameService.getGame(gameId);
    return ResponseEntity.ok(GameResponse.from(game));
  }

  @GetMapping("/me")
  public ResponseEntity<List<GameSummaryResponse>> listMyGames(
      @AuthenticationPrincipal User currentUser) {
    List<GameSummaryResponse> games =
        gameService.listUserGames(currentUser).stream().map(GameSummaryResponse::from).toList();
    return ResponseEntity.ok(games);
  }

  @PostMapping("/{gameId}/cancel")
  public ResponseEntity<GameResponse> cancelGame(@PathVariable UUID gameId) {
    Game game = gameService.cancelGame(gameId);
    return ResponseEntity.ok(GameResponse.from(game));
  }
}
