package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Exception thrown when a specific tournament participant cannot be found. */
public class TournamentParticipantNotFoundException extends BusinessException {

  /**
   * Constructs a new {@code TournamentParticipantNotFoundException} with a default message and
   * NOT_FOUND status.
   */
  public TournamentParticipantNotFoundException() {
    super("Tournament participant not found", HttpStatus.NOT_FOUND);
  }
}
