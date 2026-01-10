package com.github.solisa14.fourbagger.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for new user registration.
 *
 * <p>Enforces validation rules for username and password strength. Username must be unique (checked
 * at service layer), while password requirements are validated via annotations.
 *
 * @param username alphanumeric username (5-30 chars, allows underscores)
 * @param password strong password meeting complexity requirements (min 8 chars, uppercase,
 *     lowercase, digit, special character)
 * @param firstName optional first name
 * @param lastName optional last name
 */
public record RegisterUserRequest(
    @NotBlank(message = "Username is required")
        @Size(min = 5, max = 30, message = "Username must be between 5 and 30 characters")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username must contain only alphanumeric characters and underscores")
        String username,
    @NotBlank(message = "Password is required")
        @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message =
                "Password must be at least 8 characters long, contain at least one digit, one lowercase letter, one uppercase letter, and one special character")
        String password,
    String firstName,
    String lastName) {}
