package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserService;
import org.springframework.stereotype.Service;

/**
 * Orchestrates user authentication workflows including registration.
 *
 * <p>Coordinates between user creation and future authentication token generation. Currently
 * handles registration; authentication logic is planned for future implementation.
 */
@Service
public class AuthService {

  private final UserService userService;

  public AuthService(UserService userService) {
    this.userService = userService;
  }

  /**
   * Processes new user registration and prepares response with account details.
   *
   * <p>Delegates user creation to the UserService and constructs a response containing the user's
   * generated ID, role assignment, and timestamps.
   *
   * @param request registration details including credentials and optional profile fields
   * @return response containing the created user's public information
   */
  public RegisterUserResponse registerUser(RegisterUserRequest request) {
    User createdUser = userService.createUser(request);
    // TODO: Add authentication logic here
    // The following line is just a placeholder to return the created user details
    return new RegisterUserResponse(
        createdUser.getId(),
        createdUser.getUsername(),
        createdUser.getFirstName(),
        createdUser.getLastName(),
        createdUser.getRole(),
        createdUser.getCreatedAt());
  }
}
