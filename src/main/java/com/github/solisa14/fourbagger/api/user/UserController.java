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

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User currentUser) {
    User user = userService.getUser(currentUser.getId());
    return ResponseEntity.ok(mapToResponse(user));
  }

  @PatchMapping("/me")
  public ResponseEntity<UserResponse> updateProfile(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody UpdateProfileRequest request) {
    User updatedUser = userService.updateProfile(currentUser.getId(), request);
    return ResponseEntity.ok(mapToResponse(updatedUser));
  }

  @PutMapping("/me/password")
  public ResponseEntity<Void> updatePassword(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody UpdatePasswordRequest request) {
    userService.updatePassword(currentUser.getId(), request);
    return ResponseEntity.ok().build();
  }

  private UserResponse mapToResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getRole());
  }
}
