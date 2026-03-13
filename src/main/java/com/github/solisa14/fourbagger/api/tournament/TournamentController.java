package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/tournaments")
class TournamentController {

  private final TournamentService tournamentService;

  TournamentController(TournamentService tournamentService) {
    this.tournamentService = tournamentService;
  }

  @PostMapping
  ResponseEntity<TournamentResponse> createTournament(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody CreateTournamentRequest request) {
    Tournament tournament = tournamentService.createTournament(currentUser, request.title());
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(tournament.getId())
            .toUri();
    return ResponseEntity.created(location).body(TournamentResponse.from(tournament));
  }

  @GetMapping("/{id}")
  ResponseEntity<TournamentResponse> getTournament(@PathVariable UUID id) {
    return ResponseEntity.ok(TournamentResponse.from(tournamentService.getTournament(id)));
  }
}
