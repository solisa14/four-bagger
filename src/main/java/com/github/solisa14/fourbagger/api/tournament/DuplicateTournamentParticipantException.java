package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateTournamentParticipantException extends BusinessException {

  public DuplicateTournamentParticipantException() {
    super("User is already registered in this tournament", HttpStatus.CONFLICT);
  }
}
