package com.github.solisa14.fourbagger.api.auth;

/**
 * Command record for registering a new user.
 *
 * @param username the chosen username
 * @param email the user's email address
 * @param password the user's password
 * @param firstName the user's first name
 * @param lastName the user's last name
 */
public record RegisterUserCommand(
    String username, String email, String password, String firstName, String lastName) {}
