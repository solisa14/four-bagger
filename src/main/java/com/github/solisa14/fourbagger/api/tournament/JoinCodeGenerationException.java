package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class JoinCodeGenerationException extends BusinessException {

  public JoinCodeGenerationException() {
    super("Unable to generate a unique tournament join code", HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
