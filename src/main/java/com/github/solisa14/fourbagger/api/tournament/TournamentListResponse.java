package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;

/** Active tournaments grouped for the authenticated tournament home. */
public record TournamentListResponse(
        List<TournamentSummaryResponse> hosting, List<TournamentSummaryResponse> playing) {}
