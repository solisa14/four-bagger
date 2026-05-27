package com.github.solisa14.fourbagger.api.tournament;

import org.springframework.http.HttpStatus;
import com.github.solisa14.fourbagger.api.common.exception.BusinessException;

/**
 * Exception thrown when a user attempts to register for a tournament they are already participating
 * in.
 */
public class DuplicateTournamentParticipantException extends BusinessException {

  /**
   * Constructs a new {@code DuplicateTournamentParticipantException} with a predefined message and
   * a CONFLICT status.
   */
  public DuplicateTournamentParticipantException() {
    super("User is already registered in this tournament", HttpStatus.CONFLICT);
  }
}
