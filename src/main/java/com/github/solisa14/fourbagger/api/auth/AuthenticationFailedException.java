package com.github.solisa14.fourbagger.api.auth;

import org.springframework.http.HttpStatus;
import com.github.solisa14.fourbagger.api.common.exception.BusinessException;

/** Indicates that authentication failed due to invalid credentials or a missing account. */
public class AuthenticationFailedException extends BusinessException {

  /**
   * Constructs a new AuthenticationFailedException with a default message and UNAUTHORIZED status.
   */
  public AuthenticationFailedException() {
    super("Invalid username or password", HttpStatus.UNAUTHORIZED);
  }
}
