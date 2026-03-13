package com.github.solisa14.fourbagger.api.tournament;

import jakarta.validation.constraints.NotBlank;

public record CreateTournamentRequest(@NotBlank String title) {}
