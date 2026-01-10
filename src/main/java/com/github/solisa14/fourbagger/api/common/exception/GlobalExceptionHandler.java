package com.github.solisa14.fourbagger.api.common.exception;

import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handling for REST API endpoints.
 *
 * <p>Intercepts exceptions thrown by controllers and converts them into consistent JSON error
 * responses with appropriate HTTP status codes. Handles both business logic exceptions and
 * framework validation errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Converts application business exceptions into standardized error responses.
   *
   * <p>Extracts the HTTP status and message from the BusinessException and wraps them in an
   * ErrorResponse with the current timestamp.
   *
   * @param ex the caught business exception
   * @return response entity with the exception's status code and formatted error details
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
    ErrorResponse errorResponse =
        new ErrorResponse(Instant.now(), ex.getStatus().value(), ex.getMessage());
    return new ResponseEntity<>(errorResponse, ex.getStatus());
  }

  /**
   * Handles Jakarta Bean Validation failures on request body parameters.
   *
   * <p>Extracts the first validation error and formats it as "fieldName: error message" for the
   * client. Returns HTTP 400 Bad Request.
   *
   * @param e the validation exception containing field-level errors
   * @return response entity with HTTP 400 and the first validation error message
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    FieldError fieldError = e.getFieldErrors().getFirst();
    String message = fieldError.getField() + ": " + fieldError.getDefaultMessage();
    return new ResponseEntity<>(
        new ErrorResponse(Instant.now(), e.getStatusCode().value(), message), e.getStatusCode());
  }
}
