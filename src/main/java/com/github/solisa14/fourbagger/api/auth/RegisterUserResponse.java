package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.Role;
import java.util.UUID;

/**
 * Response payload sent to clients after successful user registration.
 *
 * <p>Contains the newly created user's public information, excluding sensitive data like passwords.
 * Returned with HTTP 201 (Created) status.
 *
 * @param id auto-generated unique identifier for the user account
 * @param username the unique username chosen during registration
 * @param email the email address associated with the account
 * @param role access level assigned to the account (typically USER for new registrations)
 */
public record RegisterUserResponse(UUID id, String username, String email, Role role) {}
