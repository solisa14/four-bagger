package com.github.solisa14.fourbagger.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a refresh token is missing, invalid, or expired.
 *
 * <p>Detail messages are surfaced to API clients via {@code GlobalExceptionHandler}; they must not
 * include the raw refresh token or other credentials.
 */
public class TokenRefreshException extends BusinessException {

  /**
   * Constructs a new TokenRefreshException with the specified detail message.
   *
   * @param message the detail message (client-visible; must not contain secrets)
   */
  public TokenRefreshException(String message) {
    super(message, HttpStatus.UNAUTHORIZED);
  }
}
