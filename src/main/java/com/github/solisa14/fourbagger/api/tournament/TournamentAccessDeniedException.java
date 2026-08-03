package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/** Exception thrown when a user is not allowed to access a tournament. */
public class TournamentAccessDeniedException extends BusinessException {

    /**
     * Constructs a new exception with the ID of the protected tournament.
     *
     * @param tournamentId the ID of the protected tournament
     */
    public TournamentAccessDeniedException(UUID tournamentId) {
        super("You are not allowed to access tournament: " + tournamentId, HttpStatus.FORBIDDEN);
    }
}
