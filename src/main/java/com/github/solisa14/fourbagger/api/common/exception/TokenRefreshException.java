package com.github.solisa14.fourbagger.api.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a refresh token is missing, invalid, or expired.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenRefreshException extends BusinessException {

  public TokenRefreshException(String token, String message) {
    super(String.format("Failed for [%s]: %s", token, message), HttpStatus.FORBIDDEN);
  }

  public TokenRefreshException(String message) {
    super(message, HttpStatus.FORBIDDEN);
  }
}
