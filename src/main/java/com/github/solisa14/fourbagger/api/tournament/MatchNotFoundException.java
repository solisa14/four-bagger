package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class MatchNotFoundException extends BusinessException {

  public MatchNotFoundException(UUID matchId) {
    super("Match not found: " + matchId, HttpStatus.NOT_FOUND);
  }
}
