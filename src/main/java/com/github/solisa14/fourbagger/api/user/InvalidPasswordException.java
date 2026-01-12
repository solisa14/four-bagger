package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends BusinessException {

  public InvalidPasswordException() {
    super("Invalid current password", HttpStatus.BAD_REQUEST);
  }
}
