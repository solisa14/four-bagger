package com.github.solisa14.fourbagger.api.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for user authentication.
 *
 * <p>Contains the credentials required to authenticate a user and generate a JWT.
 *
 * @param username the user's username
 * @param password the user's password
 */
public record LoginRequest(
    @NotBlank(message = "Username is required") String username,
    @NotBlank(message = "Password is required") String password) {}
