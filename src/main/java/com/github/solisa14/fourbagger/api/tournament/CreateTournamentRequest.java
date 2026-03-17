package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a new tournament.
 *
 * @param title the tournament title
 * @param gameType the type of tournament — {@code SINGLES} (default) or {@code DOUBLES}
 */
public record CreateTournamentRequest(@NotBlank String title, GameType gameType) {}
