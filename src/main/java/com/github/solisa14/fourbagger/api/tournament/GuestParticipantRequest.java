package com.github.solisa14.fourbagger.api.tournament;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating or renaming a guest participant on an organizer-managed tournament.
 *
 * @param displayName the guest display name (trimmed server-side; uniqueness is case-insensitive)
 */
public record GuestParticipantRequest(@NotBlank String displayName) {}
