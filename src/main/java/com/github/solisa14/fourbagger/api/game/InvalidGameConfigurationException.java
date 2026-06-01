package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a game setup request violates business rules (e.g., trying to have a player
 * play against themselves, or creating a doubles game without partners).
 */
public class InvalidGameConfigurationException extends BusinessException {

  /**
   * Constructs an exception with the specified detail message.
   *
   * @param message the detail message.
   */
  public InvalidGameConfigurationException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}
