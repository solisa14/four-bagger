package com.github.solisa14.fourbagger.api.tournament;

/**
 * Viewer-specific capabilities for tournament detail responses.
 *
 * @param canManageTournament whether the viewer is the tournament organizer
 * @param canGenerateBracket whether the viewer may generate a bracket during registration
 * @param canRemoveParticipants whether the viewer may remove participants during registration
 * @param canLeaveRegistration whether the viewer may withdraw their own registration
 * @param canOverrideMatchResults whether the organizer may override match results during play
 */
public record TournamentViewerCapabilitiesResponse(
    boolean canManageTournament,
    boolean canGenerateBracket,
    boolean canRemoveParticipants,
    boolean canLeaveRegistration,
    boolean canOverrideMatchResults) {}
