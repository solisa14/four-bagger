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
 * <p>Provides endpoints for creating, retrieving, joining, configuring, starting, and deleting
 * tournaments, as well as managing participants.
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

  /**
   * Deletes a tournament and all associated data.
   *
   * @param id the UUID of the tournament to delete
   * @return 204 No Content on success
   */
  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteTournament(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
    tournamentService.deleteTournament(id, currentUser);
    return ResponseEntity.noContent().build();
  }

  /**
   * Starts a tournament, transitioning it from BRACKET_READY to IN_PROGRESS.
   *
   * @param id the UUID of the tournament to start
   * @return the updated tournament response
   */
  @PostMapping("/{id}/start")
  ResponseEntity<TournamentResponse> startTournament(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
    tournamentService.startTournament(id, currentUser);
    return ResponseEntity.ok(
        tournamentMapper.toTournamentResponse(tournamentService.getTournament(id)));
  }

  /**
   * Generates or regenerates the bracket for a tournament. Randomly shuffles and seeds
   * participants, transitioning the tournament to BRACKET_READY.
   *
   * @param id the UUID of the tournament
   * @return the updated tournament response with bracket structure
   */
  @PostMapping("/{id}/bracket")
  ResponseEntity<TournamentResponse> generateBracket(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
    tournamentService.generateBracket(id, currentUser);
    return ResponseEntity.ok(
        tournamentMapper.toTournamentResponse(tournamentService.getTournament(id)));
  }

  /**
   * Joins the authenticated user to a tournament using a join code.
   *
   * @param currentUser the currently authenticated user
   * @param request the join request containing the tournament join code
   * @return the tournament response with updated participant list
   */
  @PostMapping("/join")
  ResponseEntity<TournamentResponse> joinTournament(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody JoinTournamentRequest request) {
    TournamentParticipant participant =
        tournamentService.joinTournament(request.joinCode(), currentUser);
    return ResponseEntity.ok(
        tournamentMapper.toTournamentResponse(
            tournamentService.getTournament(participant.getTournament().getId())));
  }

  /**
   * Removes a participant from a tournament during the registration phase.
   *
   * @param id the UUID of the tournament
   * @param participantId the UUID of the participant to remove
   * @return 204 No Content on success
   */
  @DeleteMapping("/{id}/participants/{participantId}")
  ResponseEntity<Void> removeParticipant(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID id,
      @PathVariable UUID participantId) {
    tournamentService.removeParticipant(id, currentUser, participantId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Updates round settings (best-of count and/or scoring mode) for a specific round in a tournament
   * that is in the BRACKET_READY state.
   *
   * @param id the UUID of the tournament
   * @param roundNumber the round number to configure
   * @param request the round settings to update
   * @return the updated tournament response
   */
  @PatchMapping("/{id}/rounds/{roundNumber}")
  ResponseEntity<TournamentResponse> updateRoundSettings(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID id,
      @PathVariable int roundNumber,
      @RequestBody UpdateRoundSettingsRequest request) {
    tournamentService.updateRoundSettings(
        id, currentUser, roundNumber, request.bestOf(), request.scoringMode());
    return ResponseEntity.ok(
        tournamentMapper.toTournamentResponse(tournamentService.getTournament(id)));
  }
}
