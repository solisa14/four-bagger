package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.security.JwtService;
import com.github.solisa14.fourbagger.api.user.CreateUserCommand;
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

  /**
   * Constructs an AuthenticationService.
   *
   * @param userService the user service
   * @param userRepository the user repository
   * @param authenticationManager the authentication manager
   * @param jwtService the JWT service
   * @param refreshTokenService the refresh token service
   */
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
   * @param command registration details including credentials and required profile fields
   * @return the created user entity
   */
  public User registerUser(CreateUserCommand command) {
    return userService.createUser(command);
  }

  /**
   * Authenticates a user with the provided credentials and generates a JWT and Refresh Token.
   *
   * @param command login command containing username and password
   * @return the generated token pair
   */
  public TokenPair authenticate(LoginCommand command) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(command.username(), command.password()));

    User user =
        userRepository
            .findUserByUsername(command.username())
            .orElseThrow(AuthenticationFailedException::new);

    String jwtToken = jwtService.generateToken(user);
    RefreshTokenSession refreshTokenSession = refreshTokenService.issueRefreshToken(user.getId());

    return new TokenPair(jwtToken, refreshTokenSession.rawToken());
  }

  /**
   * Rotates the refresh token and generates a new access token.
   *
   * @param requestRefreshToken the refresh token string
   * @return new JWT access token and new refresh token
   */
  public TokenPair refreshToken(String requestRefreshToken) {
    RefreshTokenSession refreshTokenSession =
        refreshTokenService.rotateRefreshToken(requestRefreshToken);
    User user = refreshTokenSession.user();
    String jwtToken = jwtService.generateToken(user);

    return new TokenPair(jwtToken, refreshTokenSession.rawToken());
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
