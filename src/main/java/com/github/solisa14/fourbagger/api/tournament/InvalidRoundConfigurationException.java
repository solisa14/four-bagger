package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Exception thrown when a tournament round configuration is invalid. */
public class InvalidRoundConfigurationException extends BusinessException {

  /**
   * Constructs a new {@code InvalidRoundConfigurationException} with the specified message.
   *
   * @param message the detail message explaining why the round configuration is invalid
   */
  public InvalidRoundConfigurationException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}
