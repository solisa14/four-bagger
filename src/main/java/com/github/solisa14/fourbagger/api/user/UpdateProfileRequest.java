package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.common.validation.AtLeastOneFieldRequired;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a user's profile.
 *
 * <p>Supports partial updates - at least one field must be provided, but both are not required.
 */
@AtLeastOneFieldRequired
public record UpdateProfileRequest(
    @Size(max = 255, message = "First name must be at most 255 characters") String firstName,
    @Size(max = 255, message = "Last name must be at most 255 characters") String lastName) {}
