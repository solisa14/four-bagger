package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Signals that user registration failed due to a duplicate username.
 *
 * <p>Thrown during account creation when the requested username is already taken. Results in an
 * HTTP 409 Conflict response to inform the client to choose a different username.
 */
public class UserAlreadyExistsException extends BusinessException {
  /**
   * Creates an exception indicating the specific username that conflicts.
   *
   * @param username the username that already exists in the system
   */
  public UserAlreadyExistsException(String username) {
    super("User with username '" + username + "' already exists", HttpStatus.CONFLICT);
  }
}
