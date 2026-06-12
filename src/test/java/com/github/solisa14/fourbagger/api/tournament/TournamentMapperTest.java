package com.github.solisa14.fourbagger.api.tournament;

import static com.github.solisa14.fourbagger.api.testsupport.TestDataFactory.user;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TournamentMapperTest {

  private final TournamentMapper mapper = new TournamentMapper();

  @Test
  void toTournamentResponse_whenResetIsInactive_hidesRoundAndIncomingRoutes() {
    Tournament tournament = tournament();
    TournamentRound finalRound = round(tournament, BracketType.FINAL);
    TournamentRound resetRound = round(tournament, BracketType.GRAND_FINAL);
    Match reset = match(resetRound, null, null);
    Match firstFinal = match(finalRound, team(tournament, "one"), team(tournament, "two"));
    firstFinal.setWinnerNextMatch(reset);
    firstFinal.setWinnerNextMatchPosition(2);
    firstFinal.setLoserNextMatch(reset);
    firstFinal.setLoserNextMatchPosition(1);
    finalRound.getMatches().add(firstFinal);
    resetRound.getMatches().add(reset);
    tournament.getRounds().addAll(List.of(finalRound, resetRound));

    TournamentResponse response = mapper.toTournamentResponse(tournament);

    assertThat(response.brackets().grandFinal()).isEmpty();
    MatchResponse firstFinalResponse =
        response.brackets().finalRounds().getFirst().matches().getFirst();
    assertThat(firstFinalResponse.winnerNextMatchId()).isNull();
    assertThat(firstFinalResponse.winnerNextMatchPosition()).isNull();
    assertThat(firstFinalResponse.loserNextMatchId()).isNull();
    assertThat(firstFinalResponse.loserNextMatchPosition()).isNull();
  }

  @Test
  void toMatchDetailResponse_mapsResultHistoryWithoutCorrectionFields() {
    Tournament tournament = tournament();
    TournamentTeam teamOne = team(tournament, "one");
    TournamentTeam teamTwo = team(tournament, "two");
    TournamentRound round = round(tournament, BracketType.WINNERS);
    Match match = match(round, teamOne, teamTwo);
    User submitter = teamOne.getPlayerOne();
    TournamentGameResult result =
        TournamentGameResult.builder()
            .id(UUID.randomUUID())
            .match(match)
            .gameNumber(1)
            .winnerTeam(teamOne)
            .teamOneScore(21)
            .teamTwoScore(15)
            .submittedBy(submitter)
            .submittedAt(Instant.parse("2026-01-01T12:00:00Z"))
            .build();

    TournamentMatchDetailResponse response =
        mapper.toMatchDetailResponse(match, List.of(result), 2);

    assertThat(response.results()).hasSize(1);
    TournamentGameResultResponse mapped = response.results().getFirst();
    assertThat(mapped.gameNumber()).isEqualTo(1);
    assertThat(mapped.winnerTeamId()).isEqualTo(teamOne.getId());
    assertThat(mapped.teamOneScore()).isEqualTo(21);
    assertThat(mapped.teamTwoScore()).isEqualTo(15);
    assertThat(mapped.submittedBy()).isEqualTo(submitter.getId());
    assertThat(mapped.submittedAt()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
    assertThat(response.nextGameNumber()).isEqualTo(2);
    assertThat(response.bestOf()).isEqualTo(1);
    assertThat(response.winsToClinch()).isEqualTo(1);
  }

  @Test
  void toTournamentResponse_whenResetIsActive_exposesRoundAndIncomingRoutes() {
    Tournament tournament = tournament();
    TournamentTeam teamOne = team(tournament, "one");
    TournamentTeam teamTwo = team(tournament, "two");
    TournamentRound finalRound = round(tournament, BracketType.FINAL);
    TournamentRound resetRound = round(tournament, BracketType.GRAND_FINAL);
    Match reset = match(resetRound, teamOne, teamTwo);
    Match firstFinal = match(finalRound, teamOne, teamTwo);
    firstFinal.setWinnerNextMatch(reset);
    firstFinal.setWinnerNextMatchPosition(2);
    firstFinal.setLoserNextMatch(reset);
    firstFinal.setLoserNextMatchPosition(1);
    finalRound.getMatches().add(firstFinal);
    resetRound.getMatches().add(reset);
    tournament.getRounds().addAll(List.of(finalRound, resetRound));

    TournamentResponse response = mapper.toTournamentResponse(tournament);

    assertThat(response.brackets().grandFinal()).hasSize(1);
    MatchResponse firstFinalResponse =
        response.brackets().finalRounds().getFirst().matches().getFirst();
    assertThat(firstFinalResponse.winnerNextMatchId()).isEqualTo(reset.getId());
    assertThat(firstFinalResponse.winnerNextMatchPosition()).isEqualTo(2);
    assertThat(firstFinalResponse.loserNextMatchId()).isEqualTo(reset.getId());
    assertThat(firstFinalResponse.loserNextMatchPosition()).isEqualTo(1);
  }

  private Tournament tournament() {
    return Tournament.builder()
        .id(UUID.randomUUID())
        .title("Tournament")
        .joinCode("ABC123")
        .status(TournamentStatus.IN_PROGRESS)
        .format(TournamentFormat.DOUBLE_ELIMINATION)
        .build();
  }

  private TournamentRound round(Tournament tournament, BracketType bracketType) {
    return TournamentRound.builder()
        .tournament(tournament)
        .bracketType(bracketType)
        .roundNumber(1)
        .bestOf(1)
        .build();
  }

  private Match match(
      TournamentRound round, TournamentTeam teamOne, TournamentTeam teamTwo) {
    return Match.builder()
        .id(UUID.randomUUID())
        .round(round)
        .matchNumber(1)
        .teamOne(teamOne)
        .teamTwo(teamTwo)
        .status(MatchStatus.PENDING)
        .build();
  }

  private TournamentTeam team(Tournament tournament, String username) {
    return TournamentTeam.builder()
        .id(UUID.randomUUID())
        .tournament(tournament)
        .playerOne(
            user(
                UUID.randomUUID(),
                username,
                "encoded",
                Role.USER))
        .build();
  }
}
