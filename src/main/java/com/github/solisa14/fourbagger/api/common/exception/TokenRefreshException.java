package com.github.solisa14.fourbagger.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a refresh token is missing, invalid, or expired.
 */
public class TokenRefreshException extends BusinessException {

  public TokenRefreshException(String token, String message) {
    super(String.format("Failed for [%s]: %s", token, message), HttpStatus.UNAUTHORIZED);
  }

  public TokenRefreshException(String message) {
    super(message, HttpStatus.UNAUTHORIZED);
  }
}
