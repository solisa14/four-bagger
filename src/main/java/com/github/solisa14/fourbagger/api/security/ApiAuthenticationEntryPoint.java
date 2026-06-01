package com.github.solisa14.fourbagger.api.security;

import com.github.solisa14.fourbagger.api.common.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Returns a consistent JSON response when a protected endpoint is accessed without authentication.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  /**
   * Constructs an ApiAuthenticationEntryPoint.
   *
   * @param objectMapper the object mapper used to serialize the error response
   */
  public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Commences an authentication scheme by writing a standard error response for unauthenticated
   * requests.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param authException the authentication exception
   * @throws IOException if an input or output exception occurs
   * @throws ServletException if a servlet exception occurs
   */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    writeErrorResponse(
        response, HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource");
  }

  private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(), new ErrorResponse(Instant.now(), status.value(), message));
  }
}
