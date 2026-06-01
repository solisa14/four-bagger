package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.user.User;

/**
 * Command record for creating a new tournament.
 *
 * @param organizer the user organizing the tournament
 * @param title the title of the tournament
 * @param gameType the type of tournament (SINGLES or DOUBLES)
 */
public record CreateTournamentCommand(User organizer, String title, GameType gameType) {}
