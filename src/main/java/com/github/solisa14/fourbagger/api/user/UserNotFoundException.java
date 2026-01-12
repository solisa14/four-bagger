package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {

  public UserNotFoundException(UUID id) {
    super("User not found with id: " + id, HttpStatus.NOT_FOUND);
  }
}
