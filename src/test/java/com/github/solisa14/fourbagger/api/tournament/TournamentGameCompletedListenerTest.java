package com.github.solisa14.fourbagger.api.tournament;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.github.solisa14.fourbagger.api.game.GameCompletedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentGameCompletedListenerTest {

  @Mock private TournamentProgressionService tournamentProgressionService;

  @InjectMocks private TournamentGameCompletedListener tournamentGameCompletedListener;

  @Test
  void onGameCompleted_whenTournamentMatchIdPresent_triggersMatchProgression() {
    UUID gameId = UUID.randomUUID();

    tournamentGameCompletedListener.onGameCompleted(
        new GameCompletedEvent(gameId, UUID.randomUUID()));

    verify(tournamentProgressionService).processCompletedGame(gameId);
  }

  @Test
  void onGameCompleted_whenTournamentMatchIdIsNull_doesNothing() {
    tournamentGameCompletedListener.onGameCompleted(
        new GameCompletedEvent(UUID.randomUUID(), null));

    verifyNoInteractions(tournamentProgressionService);
  }
}
