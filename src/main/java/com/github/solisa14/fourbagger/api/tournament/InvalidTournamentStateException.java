package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an operation is attempted on a tournament that is in an inappropriate state
 * (e.g., trying to start a tournament that is already completed).
 */
public class InvalidTournamentStateException extends BusinessException {

  /**
   * Constructs a new {@code InvalidTournamentStateException} with the specified message.
   *
   * @param message the detail message explaining the invalid state transition
   */
  public InvalidTournamentStateException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}
