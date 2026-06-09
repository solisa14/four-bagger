package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;

/**
 * Data Transfer Object grouping tournament rounds by bracket section.
 *
 * @param winners winners bracket rounds
 * @param losers losers bracket rounds
 * @param finalRounds championship final rounds
 * @param grandFinal grand-final reset rounds
 */
public record TournamentBracketsResponse(
    List<TournamentRoundResponse> winners,
    List<TournamentRoundResponse> losers,
    List<TournamentRoundResponse> finalRounds,
    List<TournamentRoundResponse> grandFinal) {

  public TournamentBracketsResponse {
    winners = List.copyOf(winners);
    losers = List.copyOf(losers);
    finalRounds = List.copyOf(finalRounds);
    grandFinal = List.copyOf(grandFinal);
  }
}
