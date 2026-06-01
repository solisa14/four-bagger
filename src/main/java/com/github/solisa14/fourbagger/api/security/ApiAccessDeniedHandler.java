package com.github.solisa14.fourbagger.api.security;

import com.github.solisa14.fourbagger.api.common.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Returns a consistent JSON response when an authenticated user lacks permission. */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  /**
   * Constructs an ApiAccessDeniedHandler.
   *
   * @param objectMapper the object mapper used to serialize the error response
   */
  public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Handles an access denied exception by writing a standard error response to the output stream.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param accessDeniedException the access denied exception
   * @throws IOException if an input or output exception occurs
   * @throws ServletException if a servlet exception occurs
   */
  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        new ErrorResponse(
            Instant.now(),
            HttpStatus.FORBIDDEN.value(),
            "You are not allowed to access this resource"));
  }
}
