package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that manages user registration and authentication endpoints.
 *
 * <p>Provides HTTP endpoints for creating new user accounts, logging in, refreshing tokens, and
 * logging out.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

  private final AuthenticationService authenticationService;
  private final JwtService jwtService;
  private final long refreshTokenDurationMs;
  private final boolean isCookieSecure;

  public AuthenticationController(
      AuthenticationService authenticationService,
      JwtService jwtService,
      @Value("${app.security.jwt.refresh-token.expiration-ms}") long refreshTokenDurationMs,
      @Value("${app.security.cookie.secure}") boolean isCookieSecure) {
    this.authenticationService = authenticationService;
    this.jwtService = jwtService;
    this.refreshTokenDurationMs = refreshTokenDurationMs;
    this.isCookieSecure = isCookieSecure;
  }

  /**
   * Registers a new user account with the provided credentials and profile information.
   *
   * <p>Validates the request payload, creates the user, logs them in, and returns HTTP 201
   * (Created) with the user details and HttpOnly authentication cookies.
   *
   * @param request validated user registration data including username, password, and required
   *     names
   * @return response entity with HTTP 201 status, the newly created user's public details, and auth
   *     cookies
   */
  @PostMapping("/register")
  public ResponseEntity<RegisterUserResponse> register(
      @Valid @RequestBody RegisterUserRequest request) {
    RegisterUserResponse response = authenticationService.registerUser(request);
    AuthenticationResponse authResponse =
        authenticationService.authenticate(
            new LoginRequest(request.username(), request.password()));

    ResponseCookie jwtCookie = createAccessTokenCookie(authResponse.accessToken());
    ResponseCookie refreshCookie = createRefreshTokenCookie(authResponse.refreshToken());

    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(response);
  }

  /**
   * Authenticates a user and sets JWT and Refresh Token in HttpOnly cookies.
   *
   * @param request login credentials
   * @return response entity with HTTP 200 and Set-Cookie headers
   */
  @PostMapping("/login")
  public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
    AuthenticationResponse authResponse = authenticationService.authenticate(request);
    ResponseCookie jwtCookie = createAccessTokenCookie(authResponse.accessToken());
    ResponseCookie refreshCookie = createRefreshTokenCookie(authResponse.refreshToken());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .build();
  }

  /**
   * Generates a new access token using a valid refresh token cookie.
   *
   * @param refreshToken the refresh token from the cookie
   * @return response entity with HTTP 200 and new Access Token cookie
   */
  @PostMapping("/refresh-token")
  public ResponseEntity<Void> refreshToken(
      @CookieValue(name = "refreshToken") String refreshToken) {
    AuthenticationResponse authResponse = authenticationService.refreshToken(refreshToken);
    ResponseCookie jwtCookie = createAccessTokenCookie(authResponse.accessToken());
    ResponseCookie refreshCookie = createRefreshTokenCookie(authResponse.refreshToken());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .build();
  }

  /**
   * Logs out the user by deleting the refresh token and clearing cookies.
   *
   * @param refreshToken the refresh token from the cookie
   * @return response entity with HTTP 200 and cleared cookies
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = "refreshToken", required = false) String refreshToken) {
    if (refreshToken != null) {
      authenticationService.logout(refreshToken);
    }
    ResponseCookie jwtCookie = createCleanCookie("accessToken");
    ResponseCookie refreshCookie = createCleanCookie("refreshToken");
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .build();
  }

  private ResponseCookie createAccessTokenCookie(String token) {
    return ResponseCookie.from("accessToken", token)
        .httpOnly(true)
        .secure(isCookieSecure)
        .path("/")
        .maxAge(jwtService.getExpirationTime() / 1000)
        .sameSite("Strict")
        .build();
  }

  private ResponseCookie createRefreshTokenCookie(String token) {
    return ResponseCookie.from("refreshToken", token)
        .httpOnly(true)
        .secure(isCookieSecure)
        .path("/api/v1/auth") // Restrict to auth endpoints
        .maxAge(refreshTokenDurationMs / 1000)
        .sameSite("Strict")
        .build();
  }

  private ResponseCookie createCleanCookie(String name) {
    return ResponseCookie.from(name, "")
        .httpOnly(true)
        .secure(isCookieSecure)
        .path(name.equals("refreshToken") ? "/api/v1/auth" : "/")
        .maxAge(0)
        .sameSite("Strict")
        .build();
  }
}
