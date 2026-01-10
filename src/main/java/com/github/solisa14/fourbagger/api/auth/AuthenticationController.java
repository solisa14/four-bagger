package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that manages user registration endpoints.
 *
 * <p>Provides HTTP endpoints for creating new user accounts with validation and proper response
 * status codes.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

  private final AuthService authService;
  private final UserService userService;

  public AuthenticationController(AuthService authService, UserService userService) {
    this.authService = authService;
    this.userService = userService;
  }

  /**
   * Registers a new user account with the provided credentials and profile information.
   *
   * <p>Validates the request payload and returns HTTP 201 (Created) on success.
   *
   * @param request validated user registration data including username, password, and optional
   *     names
   * @return response entity with HTTP 201 status and the newly created user's public details
   */
  @PostMapping
  public ResponseEntity<RegisterUserResponse> createUser(
      @Valid @RequestBody RegisterUserRequest request) {
    RegisterUserResponse response = authService.registerUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
