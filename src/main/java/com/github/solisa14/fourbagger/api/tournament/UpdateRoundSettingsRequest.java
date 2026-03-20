package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.validation.AtLeastOneFieldRequired;

/**
 * Request body for updating round configuration settings within a tournament bracket. At least one
 * field must be populated.
 *
 * @param bestOf the number of games required to win a match (1, 3, 5, or 7)
 * @param scoringMode the scoring rules for the round
 */
@AtLeastOneFieldRequired(message = "At least one round setting must be provided")
public record UpdateRoundSettingsRequest(Integer bestOf, ScoringMode scoringMode) {}
