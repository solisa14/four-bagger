package com.github.solisa14.fourbagger.api.tournament;

/**
 * Internal result of bracket eligibility evaluation for a tournament's current participant count.
 */
record BracketEligibility(
    boolean eligible,
    int participantCount,
    int minimumParticipantCount,
    boolean requiresEvenParticipantCount,
    String message) {}
