package com.github.solisa14.fourbagger.api.user;

import org.springframework.stereotype.Component;

/** Mapper for user-related entities, requests, commands, and responses. */
@Component
public class UserMapper {

  /**
   * Converts a user entity to a user response.
   *
   * @param user the user entity
   * @return the user response, or null if the user is null
   */
  public UserResponse toResponse(User user) {
    if (user == null) {
      return null;
    }
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getRole());
  }

  /**
   * Converts an update profile request to an update profile command.
   *
   * @param request the update profile request
   * @return the update profile command, or null if the request is null
   */
  public UpdateProfileCommand toCommand(UpdateProfileRequest request) {
    if (request == null) {
      return null;
    }
    return new UpdateProfileCommand(request.firstName(), request.lastName());
  }

  /**
   * Converts an update password request to an update password command.
   *
   * @param request the update password request
   * @return the update password command, or null if the request is null
   */
  public UpdatePasswordCommand toCommand(UpdatePasswordRequest request) {
    if (request == null) {
      return null;
    }
    return new UpdatePasswordCommand(request.currentPassword(), request.newPassword());
  }
}
