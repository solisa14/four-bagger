package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for tournament lifecycle management.
 *
 * <p>Provides endpoints for creating and retrieving tournaments.
 */
@RestController
@RequestMapping("/api/v1/tournaments")
class TournamentController {

  private final TournamentService tournamentService;
  private final TournamentMapper tournamentMapper;

  /**
   * Constructs a new TournamentController.
   *
   * @param tournamentService the tournament service for business logic
   * @param tournamentMapper the tournament mapper for conversion between DTOs and domain objects
   */
  TournamentController(TournamentService tournamentService, TournamentMapper tournamentMapper) {
    this.tournamentService = tournamentService;
    this.tournamentMapper = tournamentMapper;
  }

  /**
   * Creates a new tournament.
   *
   * @param currentUser the currently authenticated user organizing the tournament
   * @param request the tournament creation request
   * @return the created tournament response
   */
  @PostMapping
  ResponseEntity<TournamentResponse> createTournament(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody CreateTournamentRequest request) {
    CreateTournamentCommand command = tournamentMapper.toCreateCommand(currentUser, request);
    Tournament tournament = tournamentService.createTournament(command);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(tournament.getId())
            .toUri();
    return ResponseEntity.created(location).body(tournamentMapper.toTournamentResponse(tournament));
  }

  /**
   * Retrieves a tournament by its unique identifier.
   *
   * @param id the UUID of the tournament to retrieve
   * @return the tournament response
   */
  @GetMapping("/{id}")
  ResponseEntity<TournamentResponse> getTournament(@PathVariable UUID id) {
    return ResponseEntity.ok(
        tournamentMapper.toTournamentResponse(tournamentService.getTournament(id)));
  }
}
