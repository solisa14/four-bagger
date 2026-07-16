package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Thrown when a guest display name conflicts with another guest in the same tournament. */
public class DuplicateGuestDisplayNameException extends BusinessException {

  public DuplicateGuestDisplayNameException(String displayName) {
    super(
        "Guest display name '" + displayName + "' is already used in this tournament",
        HttpStatus.CONFLICT);
  }
}
