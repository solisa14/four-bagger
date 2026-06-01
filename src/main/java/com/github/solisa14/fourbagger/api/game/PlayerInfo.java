package com.github.solisa14.fourbagger.api.game;

import java.util.UUID;

/**
 * Data Transfer Object representing brief identifying information about a player.
 *
 * @param id The unique identifier of the player.
 * @param username The player's username.
 * @param firstName The player's first name.
 * @param lastName The player's last name.
 */
public record PlayerInfo(UUID id, String username, String firstName, String lastName) {}
