package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentProgressionServiceTest {

  @Mock private MatchRepository matchRepository;
  @Mock private SingleEliminationProgressionHandler singleEliminationProgressionHandler;
  @Mock private DoubleEliminationProgressionHandler doubleEliminationProgressionHandler;

  @InjectMocks private TournamentProgressionService tournamentProgressionService;

  @Test
  void applyGameResult_whenSeriesNotDecided_incrementsWinsAndKeepsMatchInProgress() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().setBestOf(3);
    match.setStatus(MatchStatus.IN_PROGRESS);
    TournamentGameResult result = gameResult(match, match.getTeamOne());

    tournamentProgressionService.applyGameResult(result);

    assertThat(match.getTeamOneWins()).isEqualTo(1);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    verify(matchRepository, times(2)).save(match);
    verify(singleEliminationProgressionHandler, never()).progress(any(), any(), any());
  }

  @Test
  void applyGameResult_whenBestOfFive_requiresThreeWinsToClinch() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().setBestOf(5);
    match.setStatus(MatchStatus.IN_PROGRESS);
    match.setTeamOneWins(1);
    TournamentGameResult result = gameResult(match, match.getTeamOne());

    tournamentProgressionService.applyGameResult(result);

    assertThat(match.getTeamOneWins()).isEqualTo(2);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    verify(singleEliminationProgressionHandler, never()).progress(any(), any(), any());
  }

  @Test
  void applyGameResult_whenSingleEliminationSeriesClinched_dispatchesCompletedMatch() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().setBestOf(3);
    match.setTeamOneWins(1);
    TournamentGameResult result = gameResult(match, match.getTeamOne());

    tournamentProgressionService.applyGameResult(result);

    assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(match.getWinner()).isEqualTo(match.getTeamOne());
    verify(singleEliminationProgressionHandler)
        .progress(eq(match), eq(match.getTeamOne()), eq(match.getTeamTwo()));
    verify(doubleEliminationProgressionHandler, never()).progress(any(), any(), any());
  }

  @Test
  void applyGameResult_whenDoubleEliminationSeriesClinched_dispatchesCompletedMatch() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    tournament.setFormat(TournamentFormat.DOUBLE_ELIMINATION);
    Match match = match(tournament, false);
    match.getRound().setBestOf(3);
    match.setTeamOneWins(1);
    TournamentGameResult result = gameResult(match, match.getTeamOne());

    tournamentProgressionService.applyGameResult(result);

    verify(doubleEliminationProgressionHandler)
        .progress(eq(match), eq(match.getTeamOne()), eq(match.getTeamTwo()));
    verify(singleEliminationProgressionHandler, never()).progress(any(), any(), any());
  }

  @Test
  void applyGameResult_whenMatchAlreadyCompleted_doesNotDispatchAgain() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.setStatus(MatchStatus.COMPLETED);
    TournamentGameResult result = gameResult(match, match.getTeamOne());

    tournamentProgressionService.applyGameResult(result);

    verify(matchRepository, never()).save(any());
    verify(singleEliminationProgressionHandler, never()).progress(any(), any(), any());
    verify(doubleEliminationProgressionHandler, never()).progress(any(), any(), any());
  }

  @Test
  void nextGameNumber_whenSeriesNotClinched_returnsNextPhysicalGameNumber() {
    Match match = match(tournament(TournamentStatus.IN_PROGRESS), false);
    match.setTeamOneWins(1);
    match.setTeamTwoWins(0);
    match.getRound().setBestOf(3);

    assertThat(tournamentProgressionService.nextGameNumber(match)).isEqualTo(2);
  }

  @Test
  void nextGameNumber_whenSeriesClinched_returnsNull() {
    Match match = match(tournament(TournamentStatus.IN_PROGRESS), false);
    match.getRound().setBestOf(3);
    match.setTeamOneWins(2);

    assertThat(tournamentProgressionService.nextGameNumber(match)).isNull();
  }

  private TournamentGameResult gameResult(Match match, TournamentTeam winner) {
    return TournamentGameResult.builder()
        .id(UUID.randomUUID())
        .match(match)
        .gameNumber(1)
        .winnerTeam(winner)
        .teamOneScore(21)
        .teamTwoScore(15)
        .submittedBy(user("submitter"))
        .submittedAt(Instant.now())
        .build();
  }

  private Tournament tournament(TournamentStatus status) {
    return Tournament.builder()
        .id(UUID.randomUUID())
        .organizer(user("organizer"))
        .title("Tournament")
        .status(status)
        .joinCode("ABC123")
        .build();
  }

  private Match match(Tournament tournament, boolean doubles) {
    TournamentRound round =
        TournamentRound.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .bracketType(BracketType.WINNERS)
            .roundNumber(1)
            .bestOf(1)
            .build();
    TournamentTeam teamOne =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(user("team1-a"))
            .playerTwo(doubles ? user("team1-b") : null)
            .seed(1)
            .build();
    TournamentTeam teamTwo =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(user("team2-a"))
            .playerTwo(doubles ? user("team2-b") : null)
            .seed(2)
            .build();

    return Match.builder()
        .id(UUID.randomUUID())
        .round(round)
        .teamOne(teamOne)
        .teamTwo(teamTwo)
        .matchNumber(1)
        .status(MatchStatus.PENDING)
        .build();
  }

  private User user(String suffix) {
    return TestDataFactory.user(UUID.randomUUID(), suffix, "encoded", Role.USER);
  }
}
