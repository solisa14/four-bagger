package com.github.solisa14.fourbagger.api.tournament;

/**
 * One complete manual doubles team expressed as two guest display names.
 *
 * @param playerOneDisplayName first guest name on the team
 * @param playerTwoDisplayName second guest name on the team
 */
public record ManualTeamRow(String playerOneDisplayName, String playerTwoDisplayName) {}
