package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.Role;
import java.time.Instant;
import java.util.UUID;

/**
 * Response payload sent to clients after successful user registration.
 *
 * <p>Contains the newly created user's public information, excluding sensitive data like passwords.
 * Returned with HTTP 201 (Created) status.
 *
 * @param id auto-generated unique identifier for the user account
 * @param username the unique username chosen during registration
 * @param firstName optional first name of the user
 * @param lastName optional last name of the user
 * @param role access level assigned to the account (typically USER for new registrations)
 * @param createdAt server timestamp when the account was created
 */
public record RegisterUserResponse(
    UUID id, String username, String firstName, String lastName, Role role, Instant createdAt) {}
