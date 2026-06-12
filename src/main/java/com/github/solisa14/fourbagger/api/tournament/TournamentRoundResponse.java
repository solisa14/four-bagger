package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;

/** Data Transfer Object representing a single round in the tournament bracket. */
public record TournamentRoundResponse(
    BracketType bracketType,
    int roundNumber,
    int bestOf,
    List<MatchResponse> matches) {}
