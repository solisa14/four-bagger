package com.github.solisa14.fourbagger.api.tournament;

import java.util.UUID;

/**
 * Participant summary for tournament detail responses.
 *
 * @param id the tournament participant record ID
 * @param username the account username, or null for guest participants
 * @param displayName the guest display name, or null for account participants
 * @param currentViewer whether the requesting viewer is this participant
 */
public record TournamentParticipantResponse(
    UUID id, String username, String displayName, boolean currentViewer) {}
