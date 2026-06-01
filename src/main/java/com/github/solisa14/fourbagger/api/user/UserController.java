package com.github.solisa14.fourbagger.api.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user account management endpoints.
 *
 * <p>Provides HTTP endpoints for user profile operations and account management. Profile updates
 * require both first and last names.
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

  private final UserService userService;
  private final UserMapper userMapper;

  /**
   * Constructs a new UserController.
   *
   * @param userService the user service to use
   * @param userMapper the user mapper to use
   */
  public UserController(UserService userService, UserMapper userMapper) {
    this.userService = userService;
    this.userMapper = userMapper;
  }

  /**
   * Retrieves the currently authenticated user's profile information.
   *
   * @param currentUser the currently authenticated user, injected by Spring Security
   * @return a ResponseEntity containing the user's profile data
   */
  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User currentUser) {
    User user = userService.getUser(currentUser.getId());
    return ResponseEntity.ok(userMapper.toResponse(user));
  }

  /**
   * Updates the currently authenticated user's profile information.
   *
   * @param currentUser the currently authenticated user, injected by Spring Security
   * @param request the profile update request payload
   * @return a ResponseEntity containing the updated user profile data
   */
  @PatchMapping("/me")
  public ResponseEntity<UserResponse> updateProfile(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody UpdateProfileRequest request) {
    UpdateProfileCommand command = userMapper.toCommand(request);
    User updatedUser = userService.updateProfile(currentUser.getId(), command);
    return ResponseEntity.ok(userMapper.toResponse(updatedUser));
  }

  /**
   * Updates the currently authenticated user's password.
   *
   * @param currentUser the currently authenticated user, injected by Spring Security
   * @param request the password update request payload containing current and new passwords
   * @return an empty ResponseEntity upon successful update
   */
  @PutMapping("/me/password")
  public ResponseEntity<Void> updatePassword(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody UpdatePasswordRequest request) {
    UpdatePasswordCommand command = userMapper.toCommand(request);
    userService.updatePassword(currentUser.getId(), command);
    return ResponseEntity.ok().build();
  }
}
