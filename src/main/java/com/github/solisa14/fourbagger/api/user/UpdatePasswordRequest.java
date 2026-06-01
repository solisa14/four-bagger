package com.github.solisa14.fourbagger.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for updating a user's password.
 *
 * @param currentPassword the user's current password, required for verification
 * @param newPassword the new password to set, must meet complexity requirements
 */
public record UpdatePasswordRequest(
    @NotBlank(message = "Must enter current password") String currentPassword,
    @NotBlank(message = "New password is required")
        @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message =
                "Password must be at least 8 characters long, contain at least one digit, one lowercase letter, one uppercase letter, and one special character")
        String newPassword) {}
