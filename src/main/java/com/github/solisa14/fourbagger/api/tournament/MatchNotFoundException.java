package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Exception thrown when a requested tournament match cannot be found. */
public class MatchNotFoundException extends BusinessException {

  /**
   * Constructs a new {@code MatchNotFoundException} with the specified match ID.
   *
   * @param matchId the ID of the match that could not be found
   */
  public MatchNotFoundException(UUID matchId) {
    super("Match not found: " + matchId, HttpStatus.NOT_FOUND);
  }
}
