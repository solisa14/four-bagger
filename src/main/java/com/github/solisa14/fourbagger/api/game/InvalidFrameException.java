package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a frame payload is invalid (e.g., more bags than allowed or the wrong
 * player attempts to throw in a doubles game).
 */
public class InvalidFrameException extends BusinessException {

  /**
   * Constructs an exception with the specified detail message.
   *
   * @param message the detail message.
   */
  public InvalidFrameException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}
