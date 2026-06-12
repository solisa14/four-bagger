package com.github.solisa14.fourbagger.api.tournament;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Expanded tournament match response including result history. */
public record TournamentMatchDetailResponse(
    UUID id,
    int matchNumber,
    MatchStatus status,
    boolean isBye,
    MatchResponse.TeamSummary teamOne,
    MatchResponse.TeamSummary teamTwo,
    int teamOneWins,
    int teamTwoWins,
    MatchResponse.TeamSummary winner,
    UUID winnerNextMatchId,
    Integer winnerNextMatchPosition,
    UUID loserNextMatchId,
    Integer loserNextMatchPosition,
    Instant startedAt,
    UUID startedBy,
    int bestOf,
    int winsToClinch,
    Integer nextGameNumber,
    List<TournamentGameResultResponse> results) {}
