package com.github.solisa14.fourbagger.api.tournament;

import org.springframework.http.HttpStatus;
import com.github.solisa14.fourbagger.api.common.exception.BusinessException;

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
