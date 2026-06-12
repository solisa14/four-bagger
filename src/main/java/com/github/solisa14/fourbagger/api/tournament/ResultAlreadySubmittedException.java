package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ResultAlreadySubmittedException extends BusinessException {
  public ResultAlreadySubmittedException(int gameNumber) {
    super("Result already submitted for game " + gameNumber, HttpStatus.CONFLICT);
  }
}
