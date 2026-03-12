package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Exception thrown when a user provides an incorrect current password during a password update. */
public class InvalidPasswordException extends BusinessException {

  /** Constructs a new InvalidPasswordException. */
  public InvalidPasswordException() {
    super("Invalid current password", HttpStatus.BAD_REQUEST);
  }
}
