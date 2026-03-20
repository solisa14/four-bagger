package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.Game;
import com.github.solisa14.fourbagger.api.game.GameResponse;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for tournament match operations. Provides endpoints to start matches, retrieve
 * match details, and process completed games within a tournament bracket.
 */
@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/matches")
class TournamentMatchController {

  private final TournamentMatchService tournamentMatchService;
  private final com.github.solisa14.fourbagger.api.game.GameMapper gameMapper;
  private final TournamentMapper tournamentMapper;

  /**
   * Constructs a new TournamentMatchController.
   *
   * @param tournamentMatchService the service for match-related business logic
   * @param gameMapper the game mapper for conversion between game DTOs and domain objects
   * @param tournamentMapper the tournament mapper for conversion between tournament DTOs and domain
   *     objects
   */
  TournamentMatchController(
      TournamentMatchService tournamentMatchService,
      com.github.solisa14.fourbagger.api.game.GameMapper gameMapper,
      TournamentMapper tournamentMapper) {
    this.tournamentMatchService = tournamentMatchService;
    this.gameMapper = gameMapper;
    this.tournamentMapper = tournamentMapper;
  }

  /**
   * Starts a tournament match by creating its first game. If a game already exists for this match,
   * returns the existing game.
   *
   * @param tournamentId the tournament containing the match
   * @param matchId the match to start
   * @return the created or existing game for this match
   */
  @PostMapping("/{matchId}/start")
  ResponseEntity<GameResponse> startMatch(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID tournamentId, @PathVariable UUID matchId) {
    Game game = tournamentMatchService.startMatch(tournamentId, matchId, currentUser);
    return ResponseEntity.ok(gameMapper.toGameResponse(game));
  }

  /**
   * Retrieves the current state of a tournament match including team summaries and win counts.
   *
   * @param tournamentId the tournament containing the match
   * @param matchId the match to retrieve
   * @return the match details
   */
  @GetMapping("/{matchId}")
  ResponseEntity<MatchResponse> getMatch(
      @PathVariable UUID tournamentId, @PathVariable UUID matchId) {
    Match match = tournamentMatchService.getMatch(tournamentId, matchId);
    return ResponseEntity.ok(tournamentMapper.toMatchResponse(match));
  }

}
