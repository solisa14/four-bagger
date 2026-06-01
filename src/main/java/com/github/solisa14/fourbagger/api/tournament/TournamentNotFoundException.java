package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Exception thrown when a requested tournament cannot be found in the system. */
public class TournamentNotFoundException extends BusinessException {

  /**
   * Constructs a new {@code TournamentNotFoundException} with a default message and NOT_FOUND
   * status.
   */
  public TournamentNotFoundException() {
    super("Tournament not found", HttpStatus.NOT_FOUND);
  }
}
