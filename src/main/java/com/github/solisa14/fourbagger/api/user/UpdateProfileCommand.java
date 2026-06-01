package com.github.solisa14.fourbagger.api.user;

/**
 * Command record for updating user profile information.
 *
 * @param firstName the updated first name
 * @param lastName the updated last name
 */
public record UpdateProfileCommand(String firstName, String lastName) {}
