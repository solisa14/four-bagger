package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SingleEliminationProgressionHandlerTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private MatchRepository matchRepository;
  @InjectMocks private SingleEliminationProgressionHandler handler;

  @Test
  void progress_whenWinnerRouteExists_assignsConfiguredDestinationSlot() {
    Tournament tournament = tournament();
    TournamentTeam winner = team(tournament);
    TournamentTeam loser = team(tournament);
    Match destination = match(tournament, null, null);
    Match source = match(tournament, winner, loser);
    source.setWinnerNextMatch(destination);
    source.setWinnerNextMatchPosition(2);

    handler.progress(source, winner, loser);

    assertThat(destination.getTeamTwo()).isSameAs(winner);
    assertThat(loser.getLosses()).isZero();
    assertThat(loser.isEliminated()).isFalse();
    verify(matchRepository).save(destination);
    verify(tournamentRepository, never()).save(tournament);
  }

  @Test
  void progress_whenWinnerRouteIsTerminal_completesTournamentWithoutChangingLoserState() {
    Tournament tournament = tournament();
    TournamentTeam winner = team(tournament);
    TournamentTeam loser = team(tournament);
    Match source = match(tournament, winner, loser);

    handler.progress(source, winner, loser);

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
    assertThat(loser.getLosses()).isZero();
    assertThat(loser.isEliminated()).isFalse();
    verify(tournamentRepository).save(tournament);
  }

  private Tournament tournament() {
    return Tournament.builder()
        .id(UUID.randomUUID())
        .status(TournamentStatus.IN_PROGRESS)
        .format(TournamentFormat.SINGLE_ELIMINATION)
        .build();
  }

  private TournamentTeam team(Tournament tournament) {
    return TournamentTeam.builder().id(UUID.randomUUID()).tournament(tournament).build();
  }

  private Match match(
      Tournament tournament, TournamentTeam teamOne, TournamentTeam teamTwo) {
    TournamentRound round =
        TournamentRound.builder()
            .tournament(tournament)
            .bracketType(BracketType.WINNERS)
            .roundNumber(1)
            .build();
    return Match.builder()
        .id(UUID.randomUUID())
        .round(round)
        .teamOne(teamOne)
        .teamTwo(teamTwo)
        .matchNumber(1)
        .build();
  }
}
