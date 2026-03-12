package com.github.solisa14.fourbagger.api.common.exception;

import org.springframework.http.HttpStatus;

/** Exception thrown when a refresh token is missing, invalid, or expired. */
public class TokenRefreshException extends BusinessException {

  /**
   * Constructs a new TokenRefreshException with the specified token and message.
   *
   * @param token the token that caused the exception
   * @param message the detail message
   */
  public TokenRefreshException(String token, String message) {
    super(String.format("Failed for [%s]: %s", token, message), HttpStatus.UNAUTHORIZED);
  }

  /**
   * Constructs a new TokenRefreshException with the specified detail message.
   *
   * @param message the detail message
   */
  public TokenRefreshException(String message) {
    super(message, HttpStatus.UNAUTHORIZED);
  }
}
