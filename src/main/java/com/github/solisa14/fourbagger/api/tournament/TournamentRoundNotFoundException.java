package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class TournamentRoundNotFoundException extends BusinessException {

  public TournamentRoundNotFoundException() {
    super("Tournament round not found", HttpStatus.NOT_FOUND);
  }
}
