package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.security.JwtService;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import com.github.solisa14.fourbagger.api.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Orchestrates user authentication workflows including registration and login.
 *
 * <p>Coordinates between user creation, credential verification, and token generation.
 */
@Service
public class AuthenticationService {

  private final UserService userService;
  private final UserRepository userRepository;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  public AuthenticationService(
      UserService userService,
      UserRepository userRepository,
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this.userService = userService;
    this.userRepository = userRepository;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
  }

  /**
   * Processes new user registration and prepares response with account details.
   *
   * @param request registration details including credentials and required profile fields
   * @return response containing the created user's public information
   */
  public RegisterUserResponse registerUser(RegisterUserRequest request) {
    User createdUser = userService.createUser(request);

    return new RegisterUserResponse(
        createdUser.getId(),
        createdUser.getUsername(),
        createdUser.getEmail(),
        createdUser.getRole());
  }

  /**
   * Authenticates a user with the provided credentials and generates a JWT and Refresh Token.
   *
   * @param request login request containing username and password
   * @return the generated tokens
   */
  public AuthenticationResponse authenticate(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    User user =
        userRepository
            .findUserByUsername(request.username())
            .orElseThrow(AuthenticationFailedException::new);

    String jwtToken = jwtService.generateToken(user);
    RefreshTokenSession refreshTokenSession = refreshTokenService.issueRefreshToken(user.getId());

    return new AuthenticationResponse(jwtToken, refreshTokenSession.rawToken());
  }

  /**
   * Rotates the refresh token and generates a new access token.
   *
   * @param requestRefreshToken the refresh token string
   * @return new JWT access token and new refresh token
   */
  public AuthenticationResponse refreshToken(String requestRefreshToken) {
    RefreshTokenSession refreshTokenSession = refreshTokenService.rotateRefreshToken(requestRefreshToken);
    User user = refreshTokenSession.user();
    String jwtToken = jwtService.generateToken(user);

    return new AuthenticationResponse(jwtToken, refreshTokenSession.rawToken());
  }

  /**
   * Logs out the user by deleting the refresh token.
   *
   * @param refreshToken the refresh token string
   */
  public void logout(String refreshToken) {
    refreshTokenService.deleteByToken(refreshToken);
  }
}
