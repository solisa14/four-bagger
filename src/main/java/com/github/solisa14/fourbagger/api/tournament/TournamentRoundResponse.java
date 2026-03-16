package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;

/**
 * Data Transfer Object representing a single round in the tournament bracket. Includes round
 * configuration and all matches scheduled within that round.
 *
 * @param roundNumber the round's position in the bracket (1 = first round, 2 = semis, etc.)
 * @param bestOf the maximum number of games to determine a match winner (1, 3, 5, or 7)
 * @param scoringMode the scoring rules applied to all games played in this round
 * @param matches the matches in this round, ordered by match number
 */
public record TournamentRoundResponse(
    int roundNumber, int bestOf, ScoringMode scoringMode, List<MatchResponse> matches) {}
