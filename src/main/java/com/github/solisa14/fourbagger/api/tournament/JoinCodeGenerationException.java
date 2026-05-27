package com.github.solisa14.fourbagger.api.tournament;

import org.springframework.http.HttpStatus;
import com.github.solisa14.fourbagger.api.common.exception.BusinessException;

/** Exception thrown when the system fails to generate a unique join code for a tournament. */
public class JoinCodeGenerationException extends BusinessException {

  /**
   * Constructs a new {@code JoinCodeGenerationException} with a default message and
   * INTERNAL_SERVER_ERROR status.
   */
  public JoinCodeGenerationException() {
    super("Unable to generate a unique tournament join code", HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
