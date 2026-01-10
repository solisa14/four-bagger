package com.github.solisa14.fourbagger.api.user;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user account management endpoints.
 *
 * <p>Provides HTTP endpoints for user profile operations and account management. Currently
 * placeholder for future endpoint implementations.
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }
}
