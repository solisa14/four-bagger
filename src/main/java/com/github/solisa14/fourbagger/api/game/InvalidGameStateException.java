package com.github.solisa14.fourbagger.api.game;

import org.springframework.http.HttpStatus;
import com.github.solisa14.fourbagger.api.common.exception.BusinessException;

/**
 * Exception thrown when an action cannot be performed because of the game's current status. (e.g.,
 * trying to record a frame for a game that has already finished or hasn't started).
 */
public class InvalidGameStateException extends BusinessException {

  /**
   * Constructs an exception with the specified detail message.
   *
   * @param message the detail message.
   */
  public InvalidGameStateException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}
