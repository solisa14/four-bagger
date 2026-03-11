package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidTournamentStateException extends BusinessException {

  public InvalidTournamentStateException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}
