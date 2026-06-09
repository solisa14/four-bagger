package com.github.solisa14.fourbagger.api.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.solisa14.fourbagger.api.tournament.MatchResponse;
import com.github.solisa14.fourbagger.api.tournament.ScoringMode;
import com.github.solisa14.fourbagger.api.tournament.TournamentBracketsResponse;
import com.github.solisa14.fourbagger.api.tournament.TournamentRoundResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponseImmutabilityTest {

  @Test
  void listBearingResponsesDefensivelyCopyConstructorInputs() {
    List<FrameResponse> frames = new ArrayList<>();
    GameResponse gameResponse =
        new GameResponse(
            null, null, null, null, null, null, 0, 0, 21, null, null, frames, null, null);
    frames.add(null);

    List<TournamentRoundResponse> winners = new ArrayList<>();
    TournamentBracketsResponse brackets =
        new TournamentBracketsResponse(winners, List.of(), List.of(), List.of());
    winners.add(new TournamentRoundResponse(null, 1, 1, ScoringMode.STANDARD, List.of()));

    assertThat(gameResponse.frames()).isEmpty();
    assertThat(brackets.winners()).isEmpty();
    assertThatThrownBy(() -> gameResponse.frames().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> brackets.winners().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void responseConstructorsRejectNullCollectionsAndElements() {
    assertThatThrownBy(
            () ->
                new TournamentRoundResponse(
                    null,
                    1,
                    1,
                    ScoringMode.STANDARD,
                    java.util.Arrays.asList((MatchResponse) null)))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> new TournamentBracketsResponse(null, List.of(), List.of(), List.of()))
        .isInstanceOf(NullPointerException.class);
  }
}
