package com.github.solisa14.fourbagger.api.user;

import java.util.UUID;

/**
 * Data transfer object representing a user's profile information.
 *
 * @param id the unique identifier of the user
 * @param username the user's username
 * @param email the user's email address
 * @param firstName the user's first name
 * @param lastName the user's last name
 * @param role the user's role
 */
public record UserResponse(
    UUID id, String username, String email, String firstName, String lastName, Role role) {}
