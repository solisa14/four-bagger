package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Routes completed tournament-backed games into tournament progression. */
@Component
class TournamentGameCompletedListener {

  private final TournamentProgressionService tournamentProgressionService;

  TournamentGameCompletedListener(TournamentProgressionService tournamentProgressionService) {
    this.tournamentProgressionService = tournamentProgressionService;
  }

  @EventListener
  void onGameCompleted(GameCompletedEvent event) {
    if (event.tournamentMatchId() != null) {
      tournamentProgressionService.processCompletedGame(event.gameId());
    }
  }
}
