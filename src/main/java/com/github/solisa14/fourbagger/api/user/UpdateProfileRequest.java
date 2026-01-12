package com.github.solisa14.fourbagger.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "First name is required")
        @Size(max = 255, message = "First name must be at most 255 characters")
        String firstName,
    @NotBlank(message = "Last name is required")
        @Size(max = 255, message = "Last name must be at most 255 characters")
        String lastName) {}
