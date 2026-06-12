package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ResultAlreadySubmittedException extends BusinessException {
  public ResultAlreadySubmittedException() {
    super("Game result has already been submitted", HttpStatus.CONFLICT);
  }
}
