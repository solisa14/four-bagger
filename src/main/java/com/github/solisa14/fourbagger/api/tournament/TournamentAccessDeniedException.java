package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Exception thrown when a user attempts to mutate a tournament they do not organize. */
public class TournamentAccessDeniedException extends BusinessException {

  /**
   * Constructs a new exception with the ID of the protected tournament.
   *
   * @param tournamentId the ID of the protected tournament
   */
  public TournamentAccessDeniedException(UUID tournamentId) {
    super("You are not allowed to modify tournament: " + tournamentId, HttpStatus.FORBIDDEN);
  }
}
