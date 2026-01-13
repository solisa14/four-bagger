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

  public AuthenticationService(
      UserService userService,
      UserRepository userRepository,
      AuthenticationManager authenticationManager,
      JwtService jwtService) {
    this.userService = userService;
    this.userRepository = userRepository;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  /**
   * Processes new user registration and prepares response with account details.
   *
   * @param request registration details including credentials and optional profile fields
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
   * Authenticates a user with the provided credentials and generates a JWT.
   *
   * @param request login request containing username and password
   * @return the generated JWT token
   */
  public String authenticate(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    User user = userRepository.findUserByUsername(request.username())
        .orElseThrow(); // User existence is guaranteed after successful authentication
    
    return jwtService.generateToken(user);
  }
}