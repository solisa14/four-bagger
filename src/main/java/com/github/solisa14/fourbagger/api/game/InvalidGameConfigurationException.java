package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidGameConfigurationException extends BusinessException {

  public InvalidGameConfigurationException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}
