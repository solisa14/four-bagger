package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Exception thrown when a specific tournament round cannot be found. */
public class TournamentRoundNotFoundException extends BusinessException {

  /**
   * Constructs a new {@code TournamentRoundNotFoundException} with a default message and NOT_FOUND
   * status.
   */
  public TournamentRoundNotFoundException() {
    super("Tournament round not found", HttpStatus.NOT_FOUND);
  }
}
