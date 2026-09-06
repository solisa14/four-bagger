package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.ErrorResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shared-tournaments")
class SharedTournamentController {

    private final TournamentService tournamentService;
    private final TournamentMapper tournamentMapper;

    SharedTournamentController(TournamentService tournamentService, TournamentMapper tournamentMapper) {
        this.tournamentService = tournamentService;
        this.tournamentMapper = tournamentMapper;
    }

    @GetMapping("/{shareId}")
    ResponseEntity<SharedTournamentResponse> getSharedTournament(@PathVariable String shareId) {
        UUID parsedShareId;
        try {
            parsedShareId = UUID.fromString(shareId);
        } catch (IllegalArgumentException ex) {
            throw new TournamentNotFoundException();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(tournamentMapper.toSharedTournamentResponse(
                        tournamentService.getSharedTournament(parsedShareId)));
    }

    @ExceptionHandler(TournamentNotFoundException.class)
    ResponseEntity<ErrorResponse> handleUnavailableShare() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .cacheControl(CacheControl.noStore())
                .body(new ErrorResponse(Instant.now(), HttpStatus.NOT_FOUND.value(), "Tournament not found"));
    }
}
