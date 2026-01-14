package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.common.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that manages user registration and authentication endpoints.
 *
 * <p>Provides HTTP endpoints for creating new user accounts and logging in.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

  private final AuthenticationService authenticationService;
  private final JwtService jwtService;

  public AuthenticationController(
      AuthenticationService authenticationService, JwtService jwtService) {
    this.authenticationService = authenticationService;
    this.jwtService = jwtService;
  }

  /**
   * Registers a new user account with the provided credentials and profile information.
   *
   * <p>Validates the request payload, creates the user, logs them in, and returns HTTP 201
   * (Created) with the user details and an HttpOnly authentication cookie.
   *
   * @param request validated user registration data including username, password, and optional
   *     names
   * @return response entity with HTTP 201 status, the newly created user's public details, and auth
   *     cookie
   */
  @PostMapping("/register")
  public ResponseEntity<RegisterUserResponse> register(
      @Valid @RequestBody RegisterUserRequest request) {
    RegisterUserResponse response = authenticationService.registerUser(request);
    String token =
        authenticationService.authenticate(
            new LoginRequest(request.username(), request.password()));
    ResponseCookie cookie = createAccessTokenCookie(token);
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(response);
  }

  /**
   * Authenticates a user and sets a JWT in an HttpOnly cookie.
   *
   * @param request login credentials
   * @return response entity with HTTP 200 and Set-Cookie header
   */
  @PostMapping("/login")
  public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
    String token = authenticationService.authenticate(request);
    ResponseCookie cookie = createAccessTokenCookie(token);
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
  }

  private ResponseCookie createAccessTokenCookie(String token) {
    return ResponseCookie.from("accessToken", token)
        .httpOnly(true)
        .secure(false) // Set to true in production
        .path("/")
        .maxAge(jwtService.getExpirationTime() / 1000)
        .sameSite("Strict")
        .build();
  }
}
