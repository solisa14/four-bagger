package com.github.solisa14.fourbagger.api.tournament;

/**
 * Request body for updating round configuration settings within a tournament bracket. At least one
 * field must be non-null; validation is handled by the service layer.
 *
 * @param bestOf the number of games required to win a match (1, 3, 5, or 7)
 * @param scoringMode the scoring rules for the round
 */
public record UpdateRoundSettingsRequest(Integer bestOf, ScoringMode scoringMode) {}
