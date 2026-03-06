package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class GameNotFoundException extends BusinessException {

  public GameNotFoundException(UUID gameId) {
    super("Game not found: " + gameId, HttpStatus.NOT_FOUND);
  }
}
