package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class TournamentNotFoundException extends BusinessException {

  public TournamentNotFoundException() {
    super("Tournament not found", HttpStatus.NOT_FOUND);
  }
}
