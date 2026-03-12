package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Exception thrown when a requested user cannot be found in the system. */
public class UserNotFoundException extends BusinessException {

  /**
   * Constructs a new UserNotFoundException for the specified user ID.
   *
   * @param id the UUID of the user that was not found
   */
  public UserNotFoundException(UUID id) {
    super("User not found with id: " + id, HttpStatus.NOT_FOUND);
  }
}
