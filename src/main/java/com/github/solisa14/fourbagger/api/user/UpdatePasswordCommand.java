package com.github.solisa14.fourbagger.api.user;

/**
 * Command record for updating user password.
 *
 * @param currentPassword the user's current password for verification
 * @param newPassword the new password to set
 */
public record UpdatePasswordCommand(String currentPassword, String newPassword) {}
