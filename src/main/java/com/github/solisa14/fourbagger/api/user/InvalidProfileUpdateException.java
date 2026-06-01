package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Exception thrown when a profile update request omits all updatable fields. */
public class InvalidProfileUpdateException extends BusinessException {

  /** Constructs a new InvalidProfileUpdateException. */
  public InvalidProfileUpdateException() {
    super("At least one field must be provided", HttpStatus.BAD_REQUEST);
  }
}
