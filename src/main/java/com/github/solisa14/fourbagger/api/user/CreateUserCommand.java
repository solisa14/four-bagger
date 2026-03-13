package com.github.solisa14.fourbagger.api.user;

/**
 * Command record for creating a new user.
 *
 * @param username the chosen username
 * @param email the user's email address
 * @param password the user's plaintext password
 * @param firstName the user's first name
 * @param lastName the user's last name
 */
public record CreateUserCommand(
    String username, String email, String password, String firstName, String lastName) {}
