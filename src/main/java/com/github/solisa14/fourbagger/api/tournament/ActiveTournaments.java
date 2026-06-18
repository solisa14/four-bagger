package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;

/** Active tournaments grouped by the authenticated user's relationship to them. */
public record ActiveTournaments(List<Tournament> hosting, List<Tournament> playing) {}
