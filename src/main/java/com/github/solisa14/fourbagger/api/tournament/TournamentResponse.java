package com.github.solisa14.fourbagger.api.tournament;

import java.util.UUID;

public record TournamentResponse(UUID id, String title, String joinCode, TournamentStatus status) {

  static TournamentResponse from(Tournament tournament) {
    return new TournamentResponse(
        tournament.getId(),
        tournament.getTitle(),
        tournament.getJoinCode(),
        tournament.getStatus());
  }
}
