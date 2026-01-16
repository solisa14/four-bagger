package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Signals that user registration failed due to a duplicate email.
 *
 * <p>Thrown during account creation when the requested email is already taken. Results in an HTTP
 * 409 Conflict response to inform the client to choose a different email.
 */
public class EmailAlreadyExistsException extends BusinessException {
  /**
   * Creates an exception indicating the specific email that conflicts.
   *
   * @param email the email that already exists in the system
   */
  public EmailAlreadyExistsException(String email) {
    super("Invalid email.", HttpStatus.CONFLICT);
  }
}
