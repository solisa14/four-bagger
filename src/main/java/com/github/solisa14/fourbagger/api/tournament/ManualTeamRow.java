package com.github.solisa14.fourbagger.api.tournament;

import jakarta.validation.constraints.NotBlank;

/**
 * One complete manual doubles team expressed as two guest display names.
 *
 * @param playerOneDisplayName first guest name on the team
 * @param playerTwoDisplayName second guest name on the team
 */
public record ManualTeamRow(
    @NotBlank String playerOneDisplayName, @NotBlank String playerTwoDisplayName) {}
