package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import java.util.UUID;

/** Summary of an active tournament for authenticated tournament lists. */
public record TournamentSummaryResponse(
    UUID id, String title, TournamentStatus status, TournamentFormat format, GameType gameType) {}
