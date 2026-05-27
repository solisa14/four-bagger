package com.github.solisa14.fourbagger.api.game;

import org.springframework.http.HttpStatus;
import com.github.solisa14.fourbagger.api.common.exception.BusinessException;

/** Exception thrown when a user attempts to mutate a game they are not allowed to manage. */
public class GameAccessDeniedException extends BusinessException {

  /**
   * Constructs a new exception with the ID of the game the caller cannot mutate.
   *
   * @param gameId The ID of the protected game.
   */
  public GameAccessDeniedException() {
    super("You are not allowed to access this game", HttpStatus.FORBIDDEN);
  }
}
