package com.github.solisa14.fourbagger.api.game;

import java.util.UUID;

/**
 * Application event published when a game transitions to COMPLETED status. Listeners use this to
 * react to game completion without coupling the game module to downstream concerns.
 *
 * @param gameId the ID of the completed game
 * @param tournamentMatchId the tournament match this game belongs to, or null for standalone games
 */
public record GameCompletedEvent(UUID gameId, UUID tournamentMatchId) {}
