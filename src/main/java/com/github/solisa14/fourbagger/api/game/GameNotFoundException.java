package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Exception thrown when a requested game cannot be found by its ID. */
public class GameNotFoundException extends BusinessException {

  /**
   * Constructs a new exception with the ID of the game that was not found.
   *
   * @param gameId The ID of the missing game.
   */
  public GameNotFoundException(UUID gameId) {
    super("Game not found: " + gameId, HttpStatus.NOT_FOUND);
  }
}
