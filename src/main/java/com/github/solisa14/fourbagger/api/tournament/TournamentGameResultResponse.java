package com.github.solisa14.fourbagger.api.tournament;

import java.time.Instant;
import java.util.UUID;

/** Summary of one tournament physical game result. */
public record TournamentGameResultResponse(
    int gameNumber,
    UUID winnerTeamId,
    int teamOneScore,
    int teamTwoScore,
    UUID submittedBy,
    Instant submittedAt) {}
