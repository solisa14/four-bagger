package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Exception thrown when a user is not allowed to access a tournament. */
public class TournamentAccessDeniedException extends BusinessException {

    /**
     * Constructs a new exception indicating that the user is not allowed to access a tournament.
     */
    public TournamentAccessDeniedException() {
        super("You are not allowed to access tournament", HttpStatus.FORBIDDEN);
    }
}
