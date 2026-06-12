package com.github.solisa14.fourbagger.api.user;

/**
 * Command record for creating a new user.
 *
 * @param username the chosen username
 * @param password the user's plaintext password
 * @param firstName the user's optional first name
 * @param lastName the user's optional last name
 */
public record CreateUserCommand(
    String username, String password, String firstName, String lastName) {}
