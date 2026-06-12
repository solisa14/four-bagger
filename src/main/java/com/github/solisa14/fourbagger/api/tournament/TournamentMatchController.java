package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for tournament match operations. */
@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/matches")
class TournamentMatchController {

  private final TournamentMatchService tournamentMatchService;
  private final TournamentMatchResultService tournamentMatchResultService;

  TournamentMatchController(
      TournamentMatchService tournamentMatchService,
      TournamentMatchResultService tournamentMatchResultService) {
    this.tournamentMatchService = tournamentMatchService;
    this.tournamentMatchResultService = tournamentMatchResultService;
  }

  @PostMapping("/{matchId}/start")
  ResponseEntity<TournamentMatchDetailResponse> startMatch(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID tournamentId,
      @PathVariable UUID matchId) {
    return ResponseEntity.ok(tournamentMatchService.startMatch(tournamentId, matchId, currentUser));
  }

  @GetMapping("/{matchId}")
  ResponseEntity<TournamentMatchDetailResponse> getMatch(
      @PathVariable UUID tournamentId, @PathVariable UUID matchId) {
    return ResponseEntity.ok(tournamentMatchService.getMatchDetail(tournamentId, matchId));
  }

  @PostMapping("/{matchId}/games/{gameNumber}/result")
  ResponseEntity<TournamentMatchDetailResponse> submitResult(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID tournamentId,
      @PathVariable UUID matchId,
      @PathVariable int gameNumber,
      @Valid @RequestBody SubmitTournamentGameResultRequest request) {
    return ResponseEntity.status(201)
        .body(
            tournamentMatchResultService.submitResult(
                tournamentId, matchId, gameNumber, currentUser, request));
  }
}
