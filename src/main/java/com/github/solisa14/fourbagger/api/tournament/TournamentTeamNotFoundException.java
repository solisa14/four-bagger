package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class TournamentTeamNotFoundException extends BusinessException {

  public TournamentTeamNotFoundException() {
    super("Tournament team not found", HttpStatus.NOT_FOUND);
  }
}
