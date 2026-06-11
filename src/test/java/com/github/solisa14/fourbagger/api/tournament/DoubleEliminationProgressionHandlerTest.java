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
class DoubleEliminationProgressionHandlerTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private MatchRepository matchRepository;
  @InjectMocks private DoubleEliminationProgressionHandler handler;

  @Test
  void progress_routesWinnerAndFirstTimeLoserWithoutEliminatingLoser() {
    Tournament tournament = tournament();
    TournamentTeam winner = team(tournament);
    TournamentTeam loser = team(tournament);
    Match winnerDestination = match(tournament, null, null);
    Match loserDestination = match(tournament, null, null);
    Match source = match(tournament, winner, loser);
    source.setWinnerNextMatch(winnerDestination);
    source.setWinnerNextMatchPosition(2);
    source.setLoserNextMatch(loserDestination);
    source.setLoserNextMatchPosition(1);

    handler.progress(source, winner, loser);

    assertThat(winnerDestination.getTeamTwo()).isSameAs(winner);
    assertThat(loserDestination.getTeamOne()).isSameAs(loser);
    assertThat(loser.getLosses()).isEqualTo(1);
    assertThat(loser.isEliminated()).isFalse();
    verify(matchRepository).save(winnerDestination);
    verify(matchRepository).save(loserDestination);
    verify(tournamentRepository, never()).save(tournament);
  }

  @Test
  void progress_whenLoserAlreadyHasOneLoss_marksLoserEliminated() {
    Tournament tournament = tournament();
    TournamentTeam winner = team(tournament);
    TournamentTeam loser = team(tournament);
    loser.setLosses(1);
    Match winnerDestination = match(tournament, null, null);
    Match source = match(tournament, winner, loser);
    source.setWinnerNextMatch(winnerDestination);
    source.setWinnerNextMatchPosition(1);

    handler.progress(source, winner, loser);

    assertThat(loser.getLosses()).isEqualTo(2);
    assertThat(loser.isEliminated()).isTrue();
    assertThat(winnerDestination.getTeamOne()).isSameAs(winner);
  }

  @Test
  void progress_whenMatchIsTerminal_completesTournament() {
    Tournament tournament = tournament();
    TournamentTeam winner = team(tournament);
    TournamentTeam loser = team(tournament);
    loser.setLosses(1);
    Match source = match(tournament, winner, loser);

    handler.progress(source, winner, loser);

    assertThat(loser.getLosses()).isEqualTo(2);
    assertThat(loser.isEliminated()).isTrue();
    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
    verify(tournamentRepository).save(tournament);
  }

  private Tournament tournament() {
    return Tournament.builder()
        .id(UUID.randomUUID())
        .status(TournamentStatus.IN_PROGRESS)
        .format(TournamentFormat.DOUBLE_ELIMINATION)
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
