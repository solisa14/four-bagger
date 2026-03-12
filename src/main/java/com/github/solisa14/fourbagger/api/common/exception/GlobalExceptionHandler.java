package com.github.solisa14.fourbagger.api.common.exception;

import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
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
    return buildResponse(ex.getStatus(), ex.getMessage());
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
    String message;
    if (!e.getFieldErrors().isEmpty()) {
      FieldError fieldError = e.getFieldErrors().getFirst();
      message = fieldError.getField() + ": " + fieldError.getDefaultMessage();
    } else {
      message = e.getAllErrors().getFirst().getDefaultMessage();
    }
    return buildResponse(HttpStatus.valueOf(e.getStatusCode().value()), message);
  }

  /**
   * Handles authentication failures.
   *
   * <p>Returns HTTP 401 Unauthorized when authentication fails (e.g., invalid credentials).
   *
   * @param ex the authentication exception
   * @return response entity with HTTP 401 and a generic error message
   */
  @ExceptionHandler({AuthenticationException.class})
  public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
    return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
  }

  /**
   * Handles token refresh exceptions.
   *
   * <p>Returns HTTP 403 Forbidden when a token refresh fails (e.g., expired or invalid token).
   *
   * @param ex the token refresh exception
   * @return response entity with HTTP 403 and the error message
   */
  @ExceptionHandler(TokenRefreshException.class)
  public ResponseEntity<ErrorResponse> handleTokenRefreshException(TokenRefreshException ex) {
    return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  /**
   * Handles missing request cookie exceptions.
   *
   * <p>Returns HTTP 401 Unauthorized when a required cookie is missing from the request.
   *
   * @param ex the missing request cookie exception
   * @return response entity with HTTP 401 and an error message specifying the missing cookie
   */
  @ExceptionHandler(MissingRequestCookieException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestCookieException(
      MissingRequestCookieException ex) {
    String message =
        switch (ex.getCookieName()) {
          case "refreshToken" -> "Refresh token is required";
          case "accessToken" -> "Access token is required";
          default -> ex.getCookieName() + " cookie is required";
        };
    return buildResponse(HttpStatus.UNAUTHORIZED, message);
  }

  /**
   * Handles data integrity violation exceptions.
   *
   * <p>Returns HTTP 409 Conflict when a database operation violates integrity constraints.
   *
   * @param ex the data integrity violation exception
   * @return response entity with HTTP 409 and a conflict error message
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    return buildResponse(HttpStatus.CONFLICT, "Request conflicts with existing data");
  }

  private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
    ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), message);
    return new ResponseEntity<>(errorResponse, status);
  }
}
