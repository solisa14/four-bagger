package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a new tournament.
 *
 * @param title the tournament title
 * @param gameType the type of tournament — {@code SINGLES} (default) or {@code DOUBLES}
 * @param participationMode how players enter; required with no server-side default
 */
public record CreateTournamentRequest(
    @NotBlank String title,
    GameType gameType,
    TournamentFormat format,
    @NotNull TournamentParticipationMode participationMode) {}
