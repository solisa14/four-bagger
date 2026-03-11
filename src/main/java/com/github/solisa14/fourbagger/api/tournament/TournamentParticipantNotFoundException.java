package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class TournamentParticipantNotFoundException extends BusinessException {

  public TournamentParticipantNotFoundException() {
    super("Tournament participant not found", HttpStatus.NOT_FOUND);
  }
}
