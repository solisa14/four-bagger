package com.github.solisa14.fourbagger.api.tournament;

import org.springframework.http.HttpStatus;
import com.github.solisa14.fourbagger.api.common.exception.BusinessException;

/** Exception thrown when a specific tournament team cannot be found. */
public class TournamentTeamNotFoundException extends BusinessException {

  /**
   * Constructs a new {@code TournamentTeamNotFoundException} with a default message and NOT_FOUND
   * status.
   */
  public TournamentTeamNotFoundException() {
    super("Tournament team not found", HttpStatus.NOT_FOUND);
  }
}
