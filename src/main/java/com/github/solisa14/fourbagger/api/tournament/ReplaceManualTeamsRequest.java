package com.github.solisa14.fourbagger.api.tournament;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request body that replaces the full set of manual doubles teams during registration.
 *
 * @param teams complete two-guest team rows; every guest must appear in exactly one team
 */
public record ReplaceManualTeamsRequest(@NotEmpty List<@Valid @NotNull ManualTeamRow> teams) {}
