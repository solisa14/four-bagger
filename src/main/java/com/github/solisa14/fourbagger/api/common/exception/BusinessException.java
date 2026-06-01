package com.github.solisa14.fourbagger.api.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for application business logic errors that map to HTTP responses.
 *
 * <p>Extends RuntimeException to avoid forced exception handling while carrying HTTP status codes
 * for the GlobalExceptionHandler to convert into appropriate REST responses.
 */
@Getter
public class BusinessException extends RuntimeException {
  private final HttpStatus status;

  /**
   * Creates an exception with a user-facing message and corresponding HTTP status.
   *
   * @param message descriptive error message to include in the API response
   * @param status HTTP status code to return (e.g., 400 BAD_REQUEST, 409 CONFLICT)
   */
  public BusinessException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }
}
