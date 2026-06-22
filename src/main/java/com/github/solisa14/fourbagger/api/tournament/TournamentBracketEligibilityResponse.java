package com.github.solisa14.fourbagger.api.tournament;

/**
 * Bracket eligibility summary for tournament detail responses.
 *
 * @param eligible whether participant requirements are met for bracket generation
 * @param participantCount current number of registered participants
 * @param minimumParticipantCount minimum participants required for this tournament type
 * @param requiresEvenParticipantCount whether an even participant count is required
 * @param message user-facing eligibility explanation
 */
public record TournamentBracketEligibilityResponse(
    boolean eligible,
    int participantCount,
    int minimumParticipantCount,
    boolean requiresEvenParticipantCount,
    String message) {}
