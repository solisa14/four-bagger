package com.github.solisa14.fourbagger.api.auth;

/**
 * Command record for user login.
 *
 * @param username the user's username
 * @param password the user's password
 */
public record LoginCommand(String username, String password) {}
