package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.CreateUserCommand;
import com.github.solisa14.fourbagger.api.user.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for authentication-related requests and commands. Provides conversions between requests,
 * commands, and responses.
 */
@Component
public class AuthMapper {

  /**
   * Converts a registration request to a user creation command.
   *
   * @param request the registration request
   * @return the user creation command, or null if the request is null
   */
  public CreateUserCommand toCommand(RegisterUserRequest request) {
    if (request == null) {
      return null;
    }
    return new CreateUserCommand(
        request.username(),
        request.email(),
        request.password(),
        request.firstName(),
        request.lastName());
  }

  /**
   * Converts a login request to a login command.
   *
   * @param request the login request
   * @return the login command, or null if the request is null
   */
  public LoginCommand toCommand(LoginRequest request) {
    if (request == null) {
      return null;
    }
    return new LoginCommand(request.username(), request.password());
  }

  /**
   * Converts a user entity to a registration response.
   *
   * @param user the user entity
   * @return the registration response, or null if the user is null
   */
  public RegisterUserResponse toRegisterResponse(User user) {
    if (user == null) {
      return null;
    }
    return new RegisterUserResponse(
        user.getId(), user.getUsername(), user.getEmail(), user.getRole());
  }
}
