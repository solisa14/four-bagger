package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestCookieHelper;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests verifying tournament match progression driven by final score result
 * submissions.
 */
class TournamentGameProgressionIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private TournamentService tournamentService;
  @Autowired private MatchRepository matchRepository;
  @Autowired private UserRepository userRepository;

  @Test
  void submitResult_whenFourTeamDoubleEliminationBracketCompletes_progressesEntireGraph()
      throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("deorg" + suffix);
    registerAndGetToken("dep1" + suffix);
    registerAndGetToken("dep2" + suffix);
    registerAndGetToken("dep3" + suffix);
    registerAndGetToken("dep4" + suffix);

    User organizer = userRepository.findUserByUsername("deorg" + suffix + "user").orElseThrow();
    User player1 = userRepository.findUserByUsername("dep1" + suffix + "user").orElseThrow();
    User player2 = userRepository.findUserByUsername("dep2" + suffix + "user").orElseThrow();
    User player3 = userRepository.findUserByUsername("dep3" + suffix + "user").orElseThrow();
    User player4 = userRepository.findUserByUsername("dep4" + suffix + "user").orElseThrow();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Double Elimination Progression",
                                null,
                                TournamentFormat.DOUBLE_ELIMINATION))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("DOUBLE_ELIMINATION"))
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.joinTournament(joinCode, player4);
    tournamentService.generateBracket(tournamentId, organizer);
    tournamentService.startTournament(tournamentId, organizer);

    List<Match> matches = tournamentMatches(tournamentId);
    assertThat(matches).hasSize(7);
    Match winnerRoundOneMatchOne = match(matches, BracketType.WINNERS, 1, 1);
    Match winnerRoundOneMatchTwo = match(matches, BracketType.WINNERS, 1, 2);

    completeMatchWithTeamOneWin(tournamentId, winnerRoundOneMatchOne.getId(), orgToken);
    completeMatchWithTeamOneWin(tournamentId, winnerRoundOneMatchTwo.getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    winnerRoundOneMatchOne = match(matches, BracketType.WINNERS, 1, 1);
    winnerRoundOneMatchTwo = match(matches, BracketType.WINNERS, 1, 2);
    Match winnerFinal = match(matches, BracketType.WINNERS, 2, 1);
    Match loserRoundOne = match(matches, BracketType.LOSERS, 1, 1);

    assertThat(winnerFinal.getTeamOne().getId())
        .isEqualTo(winnerRoundOneMatchOne.getWinner().getId());
    assertThat(winnerFinal.getTeamTwo().getId())
        .isEqualTo(winnerRoundOneMatchTwo.getWinner().getId());
    assertThat(loserRoundOne.getTeamOne().getId())
        .isEqualTo(winnerRoundOneMatchOne.getTeamTwo().getId());
    assertThat(loserRoundOne.getTeamTwo().getId())
        .isEqualTo(winnerRoundOneMatchTwo.getTeamTwo().getId());
    assertThat(loserRoundOne.getTeamOne().getLosses()).isEqualTo(1);
    assertThat(loserRoundOne.getTeamTwo().getLosses()).isEqualTo(1);

    UUID firstEliminatedTeamId = loserRoundOne.getTeamTwo().getId();
    completeMatchWithTeamOneWin(tournamentId, loserRoundOne.getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    TournamentTeam firstEliminatedTeam = team(matches, firstEliminatedTeamId);
    assertThat(firstEliminatedTeam.getLosses()).isEqualTo(2);
    assertThat(firstEliminatedTeam.isEliminated()).isTrue();

    winnerFinal = match(matches, BracketType.WINNERS, 2, 1);
    UUID winnerFinalLoserId = winnerFinal.getTeamTwo().getId();
    completeMatchWithTeamOneWin(tournamentId, winnerFinal.getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    Match loserFinal = match(matches, BracketType.LOSERS, 2, 1);
    Match championship = match(matches, BracketType.FINAL, 1, 1);
    assertThat(loserFinal.getTeamOne().getId())
        .isEqualTo(match(matches, BracketType.LOSERS, 1, 1).getWinner().getId());
    assertThat(loserFinal.getTeamTwo().getId()).isEqualTo(winnerFinalLoserId);
    assertThat(loserFinal.getTeamTwo().getLosses()).isEqualTo(1);
    assertThat(championship.getTeamOne().getId())
        .isEqualTo(match(matches, BracketType.WINNERS, 2, 1).getWinner().getId());

    completeMatchWithTeamOneWin(tournamentId, loserFinal.getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    TournamentTeam secondEliminatedTeam = team(matches, winnerFinalLoserId);
    championship = match(matches, BracketType.FINAL, 1, 1);
    assertThat(secondEliminatedTeam.getLosses()).isEqualTo(2);
    assertThat(secondEliminatedTeam.isEliminated()).isTrue();
    assertThat(championship.getTeamTwo().getId())
        .isEqualTo(match(matches, BracketType.LOSERS, 2, 1).getWinner().getId());
    assertThat(championship.getTeamTwo().getLosses()).isEqualTo(1);

    UUID championshipLoserId = championship.getTeamTwo().getId();
    completeMatchWithTeamOneWin(tournamentId, championship.getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    championship = match(matches, BracketType.FINAL, 1, 1);
    TournamentTeam championshipLoser = team(matches, championshipLoserId);
    assertThat(championship.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(championship.getWinner().getId()).isEqualTo(championship.getTeamOne().getId());
    assertThat(championshipLoser.getLosses()).isEqualTo(2);
    assertThat(championshipLoser.isEliminated()).isTrue();

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.brackets.grandFinal").isEmpty())
        .andExpect(jsonPath("$.brackets.finalRounds[0].matches[0].winnerNextMatchId").isEmpty())
        .andExpect(jsonPath("$.brackets.finalRounds[0].matches[0].loserNextMatchId").isEmpty());
  }

  @Test
  void submitResult_whenFourTeamDoublesDoubleEliminationBracketCompletes_progressesEntireGraph()
      throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("dedorg" + suffix);
    User organizer =
        userRepository.findUserByUsername("dedorg" + suffix + "user").orElseThrow();
    User[] players = new User[8];
    for (int i = 0; i < players.length; i++) {
      String usernamePrefix = "dedp" + i + suffix;
      registerAndGetToken(usernamePrefix);
      players[i] = userRepository.findUserByUsername(usernamePrefix + "user").orElseThrow();
    }

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Doubles Double Elimination Progression",
                                GameType.DOUBLES,
                                TournamentFormat.DOUBLE_ELIMINATION))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.gameType").value("DOUBLES"))
            .andExpect(jsonPath("$.format").value("DOUBLE_ELIMINATION"))
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    for (User player : players) {
      tournamentService.joinTournament(joinCode, player);
    }
    tournamentService.generateBracket(tournamentId, organizer);
    tournamentService.startTournament(tournamentId, organizer);

    List<Match> matches = tournamentMatches(tournamentId);
    assertThat(matches).hasSize(7);
    List<TournamentTeam> initialTeams =
        matches.stream()
            .filter(candidate -> candidate.getRound().getBracketType() == BracketType.WINNERS)
            .filter(candidate -> candidate.getRound().getRoundNumber() == 1)
            .flatMap(
                candidate ->
                    java.util.stream.Stream.of(candidate.getTeamOne(), candidate.getTeamTwo()))
            .toList();
    assertThat(initialTeams).hasSize(4);
    assertThat(initialTeams).extracting(TournamentTeam::getPlayerOne).doesNotContainNull();
    assertThat(initialTeams).extracting(TournamentTeam::getPlayerTwo).doesNotContainNull();
    assertThat(
            initialTeams.stream()
                .flatMap(
                    team ->
                        java.util.stream.Stream.of(
                            team.getPlayerOne().getId(), team.getPlayerTwo().getId()))
                .distinct())
        .hasSize(8);

    Match winnerRoundOneMatchOne = match(matches, BracketType.WINNERS, 1, 1);
    Match winnerRoundOneMatchTwo = match(matches, BracketType.WINNERS, 1, 2);
    completeDoublesMatch(tournamentId, winnerRoundOneMatchOne.getId(), orgToken, true);
    completeDoublesMatch(tournamentId, winnerRoundOneMatchTwo.getId(), orgToken, true);

    matches = tournamentMatches(tournamentId);
    winnerRoundOneMatchOne = match(matches, BracketType.WINNERS, 1, 1);
    winnerRoundOneMatchTwo = match(matches, BracketType.WINNERS, 1, 2);
    Match winnerFinal = match(matches, BracketType.WINNERS, 2, 1);
    Match loserRoundOne = match(matches, BracketType.LOSERS, 1, 1);
    assertThat(winnerFinal.getTeamOne().getId())
        .isEqualTo(winnerRoundOneMatchOne.getWinner().getId());
    assertThat(winnerFinal.getTeamTwo().getId())
        .isEqualTo(winnerRoundOneMatchTwo.getWinner().getId());
    assertThat(loserRoundOne.getTeamOne().getId())
        .isEqualTo(winnerRoundOneMatchOne.getTeamTwo().getId());
    assertThat(loserRoundOne.getTeamTwo().getId())
        .isEqualTo(winnerRoundOneMatchTwo.getTeamTwo().getId());

    UUID firstEliminatedTeamId = loserRoundOne.getTeamTwo().getId();
    completeDoublesMatch(tournamentId, loserRoundOne.getId(), orgToken, true);

    matches = tournamentMatches(tournamentId);
    TournamentTeam firstEliminatedTeam = team(matches, firstEliminatedTeamId);
    assertThat(firstEliminatedTeam.getLosses()).isEqualTo(2);
    assertThat(firstEliminatedTeam.isEliminated()).isTrue();

    winnerFinal = match(matches, BracketType.WINNERS, 2, 1);
    UUID winnerFinalLoserId = winnerFinal.getTeamTwo().getId();
    completeDoublesMatch(tournamentId, winnerFinal.getId(), orgToken, true);

    matches = tournamentMatches(tournamentId);
    Match loserFinal = match(matches, BracketType.LOSERS, 2, 1);
    Match championship = match(matches, BracketType.FINAL, 1, 1);
    assertThat(loserFinal.getTeamOne().getId())
        .isEqualTo(match(matches, BracketType.LOSERS, 1, 1).getWinner().getId());
    assertThat(loserFinal.getTeamTwo().getId()).isEqualTo(winnerFinalLoserId);
    assertThat(championship.getTeamOne().getId())
        .isEqualTo(match(matches, BracketType.WINNERS, 2, 1).getWinner().getId());

    completeDoublesMatch(tournamentId, loserFinal.getId(), orgToken, true);

    matches = tournamentMatches(tournamentId);
    TournamentTeam secondEliminatedTeam = team(matches, winnerFinalLoserId);
    championship = match(matches, BracketType.FINAL, 1, 1);
    assertThat(secondEliminatedTeam.getLosses()).isEqualTo(2);
    assertThat(secondEliminatedTeam.isEliminated()).isTrue();
    assertThat(championship.getTeamTwo().getId())
        .isEqualTo(match(matches, BracketType.LOSERS, 2, 1).getWinner().getId());

    UUID championshipLoserId = championship.getTeamTwo().getId();
    completeDoublesMatch(tournamentId, championship.getId(), orgToken, true);

    matches = tournamentMatches(tournamentId);
    championship = match(matches, BracketType.FINAL, 1, 1);
    TournamentTeam championshipLoser = team(matches, championshipLoserId);
    assertThat(championship.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(championship.getWinner().getId()).isEqualTo(championship.getTeamOne().getId());
    assertThat(championshipLoser.getLosses()).isEqualTo(2);
    assertThat(championshipLoser.isEliminated()).isTrue();

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameType").value("DOUBLES"))
        .andExpect(jsonPath("$.format").value("DOUBLE_ELIMINATION"))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.brackets.grandFinal").isEmpty());
  }

  @Test
  void submitResult_whenLosersBracketFinalistWinsFirstFinal_activatesAndCompletesReset()
      throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("resetorg" + suffix);
    registerAndGetToken("resetp1" + suffix);
    registerAndGetToken("resetp2" + suffix);
    registerAndGetToken("resetp3" + suffix);
    registerAndGetToken("resetp4" + suffix);

    User organizer =
        userRepository.findUserByUsername("resetorg" + suffix + "user").orElseThrow();
    User player1 =
        userRepository.findUserByUsername("resetp1" + suffix + "user").orElseThrow();
    User player2 =
        userRepository.findUserByUsername("resetp2" + suffix + "user").orElseThrow();
    User player3 =
        userRepository.findUserByUsername("resetp3" + suffix + "user").orElseThrow();
    User player4 =
        userRepository.findUserByUsername("resetp4" + suffix + "user").orElseThrow();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Reset Final Progression",
                                null,
                                TournamentFormat.DOUBLE_ELIMINATION))))
            .andExpect(status().isCreated())
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.joinTournament(joinCode, player4);
    tournamentService.generateBracket(tournamentId, organizer);
    tournamentService.startTournament(tournamentId, organizer);

    List<Match> matches = tournamentMatches(tournamentId);
    completeMatchWithTeamOneWin(
        tournamentId, match(matches, BracketType.WINNERS, 1, 1).getId(), orgToken);
    completeMatchWithTeamOneWin(
        tournamentId, match(matches, BracketType.WINNERS, 1, 2).getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    completeMatchWithTeamOneWin(
        tournamentId, match(matches, BracketType.LOSERS, 1, 1).getId(), orgToken);
    completeMatchWithTeamOneWin(
        tournamentId, match(matches, BracketType.WINNERS, 2, 1).getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    completeMatchWithTeamOneWin(
        tournamentId, match(matches, BracketType.LOSERS, 2, 1).getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    Match firstFinal = match(matches, BracketType.FINAL, 1, 1);
    UUID undefeatedFinalistId = firstFinal.getTeamOne().getId();
    UUID oneLossFinalistId = firstFinal.getTeamTwo().getId();
    completeMatchWithTeamTwoWin(tournamentId, firstFinal.getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    Match resetFinal = match(matches, BracketType.GRAND_FINAL, 1, 1);
    assertThat(resetFinal.getTeamOne().getId()).isEqualTo(undefeatedFinalistId);
    assertThat(resetFinal.getTeamTwo().getId()).isEqualTo(oneLossFinalistId);
    assertThat(resetFinal.getTeamOne().getLosses()).isEqualTo(1);
    assertThat(resetFinal.getTeamTwo().getLosses()).isEqualTo(1);
    assertThat(resetFinal.getRound().getBestOf()).isEqualTo(1);

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.brackets.grandFinal").isNotEmpty())
        .andExpect(jsonPath("$.brackets.finalRounds[0].matches[0].winnerNextMatchId").isNotEmpty())
        .andExpect(jsonPath("$.brackets.finalRounds[0].matches[0].loserNextMatchId").isNotEmpty());

    completeMatchWithTeamOneWin(tournamentId, resetFinal.getId(), orgToken);

    matches = tournamentMatches(tournamentId);
    resetFinal = match(matches, BracketType.GRAND_FINAL, 1, 1);
    TournamentTeam eliminatedFinalist = team(matches, oneLossFinalistId);
    assertThat(resetFinal.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(resetFinal.getWinner().getId()).isEqualTo(undefeatedFinalistId);
    assertThat(eliminatedFinalist.getLosses()).isEqualTo(2);
    assertThat(eliminatedFinalist.isEliminated()).isTrue();

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void submitResult_whenAllMatchesComplete_completesSingleEliminationTournament()
      throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("fullorg" + suffix);
    registerAndGetToken("fullp1" + suffix);
    registerAndGetToken("fullp2" + suffix);
    registerAndGetToken("fullp3" + suffix);
    registerAndGetToken("fullp4" + suffix);

    User organizer = userRepository.findUserByUsername("fullorg" + suffix + "user").orElseThrow();
    User player1 = userRepository.findUserByUsername("fullp1" + suffix + "user").orElseThrow();
    User player2 = userRepository.findUserByUsername("fullp2" + suffix + "user").orElseThrow();
    User player3 = userRepository.findUserByUsername("fullp3" + suffix + "user").orElseThrow();
    User player4 = userRepository.findUserByUsername("fullp4" + suffix + "user").orElseThrow();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Full Progression Test",
                                null,
                                TournamentFormat.SINGLE_ELIMINATION))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.joinTournament(joinCode, player4);
    tournamentService.generateBracket(tournamentId, organizer);
    tournamentService.startTournament(tournamentId, organizer);

    List<Match> initialMatches =
        matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(
            tournamentId);
    assertThat(initialMatches).hasSize(3);
    UUID firstSemifinalId = initialMatches.get(0).getId();
    UUID secondSemifinalId = initialMatches.get(1).getId();
    UUID finalMatchId = initialMatches.get(2).getId();

    completeMatchWithTeamOneWin(tournamentId, firstSemifinalId, orgToken);
    completeMatchWithTeamOneWin(tournamentId, secondSemifinalId, orgToken);

    List<Match> matchesAfterSemifinals =
        matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(
            tournamentId);
    Match firstSemifinal = matchesAfterSemifinals.get(0);
    Match secondSemifinal = matchesAfterSemifinals.get(1);
    Match finalMatch = matchesAfterSemifinals.get(2);

    assertThat(firstSemifinal.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(secondSemifinal.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(finalMatch.getTeamOne().getId()).isEqualTo(firstSemifinal.getWinner().getId());
    assertThat(finalMatch.getTeamTwo().getId()).isEqualTo(secondSemifinal.getWinner().getId());

    completeMatchWithTeamOneWin(tournamentId, finalMatchId, orgToken);

    List<Match> completedMatches =
        matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(
            tournamentId);
    Match completedFinal = completedMatches.get(2);
    assertThat(completedFinal.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(completedFinal.getWinner()).isNotNull();

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void submitResult_whenOrganizerIsNotPlayer_areStillAllowed() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("mutorg" + suffix);
    registerAndGetToken("mutp1" + suffix);
    registerAndGetToken("mutp2" + suffix);
    registerAndGetToken("mutp3" + suffix);

    User organizer = userRepository.findUserByUsername("mutorg" + suffix + "user").orElseThrow();
    User player1 = userRepository.findUserByUsername("mutp1" + suffix + "user").orElseThrow();
    User player2 = userRepository.findUserByUsername("mutp2" + suffix + "user").orElseThrow();
    User player3 = userRepository.findUserByUsername("mutp3" + suffix + "user").orElseThrow();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Mutation Test", null, TournamentFormat.SINGLE_ELIMINATION))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.generateBracket(tournamentId, organizer);
    tournamentService.startTournament(tournamentId, organizer);

    UUID matchId =
        matchRepository
            .findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(tournamentId)
            .stream()
            .filter(match -> !match.isBye())
            .findFirst()
            .orElseThrow()
            .getId();

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nextGameNumber").value(1));

    MvcResult detailResult =
        mockMvc
            .perform(
                get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
            .andExpect(status().isOk())
            .andReturn();

    var detailJson = objectMapper.readTree(detailResult.getResponse().getContentAsString());
    UUID teamOneId = UUID.fromString(detailJson.get("teamOne").get("id").asText());

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new SubmitTournamentGameResultRequest(teamOneId, 21, 15))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.results[0].teamOneScore").value(21))
        .andExpect(jsonPath("$.results[0].teamTwoScore").value(15));
  }

  @Test
  void submitResult_whenExactRetry_returnsSuccessWithoutDoubleProgression() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("retryorg" + suffix);
    registerAndGetToken("retryp1" + suffix);
    registerAndGetToken("retryp2" + suffix);
    registerAndGetToken("retryp3" + suffix);

    User organizer = userRepository.findUserByUsername("retryorg" + suffix + "user").orElseThrow();
    User player1 = userRepository.findUserByUsername("retryp1" + suffix + "user").orElseThrow();
    User player2 = userRepository.findUserByUsername("retryp2" + suffix + "user").orElseThrow();
    User player3 = userRepository.findUserByUsername("retryp3" + suffix + "user").orElseThrow();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Retry Test", null, TournamentFormat.SINGLE_ELIMINATION))))
            .andExpect(status().isCreated())
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.generateBracket(tournamentId, organizer);
    tournamentService.startTournament(tournamentId, organizer);

    UUID matchId =
        matchRepository
            .findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(tournamentId)
            .stream()
            .filter(match -> !match.isBye())
            .findFirst()
            .orElseThrow()
            .getId();

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk());

    MvcResult detailResult =
        mockMvc
            .perform(
                get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
            .andExpect(status().isOk())
            .andReturn();

    var detailJson = objectMapper.readTree(detailResult.getResponse().getContentAsString());
    UUID teamOneId = UUID.fromString(detailJson.get("teamOne").get("id").asText());
    SubmitTournamentGameResultRequest request =
        new SubmitTournamentGameResultRequest(teamOneId, 21, 15);

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.teamOneWins").value(1))
        .andExpect(jsonPath("$.results").isArray())
        .andExpect(jsonPath("$.results.length()").value(1));

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.teamOneWins").value(1))
        .andExpect(jsonPath("$.results.length()").value(1));

    Match persistedMatch = matchRepository.findById(matchId).orElseThrow();
    assertThat(persistedMatch.getTeamOneWins()).isEqualTo(1);
    assertThat(persistedMatch.getTeamTwoWins()).isEqualTo(0);
  }

  @Test
  void submitResult_whenConflictingRetry_returnsConflict() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("conflictorg" + suffix);
    registerAndGetToken("conflictp1" + suffix);
    registerAndGetToken("conflictp2" + suffix);
    registerAndGetToken("conflictp3" + suffix);

    User organizer =
        userRepository.findUserByUsername("conflictorg" + suffix + "user").orElseThrow();
    User player1 =
        userRepository.findUserByUsername("conflictp1" + suffix + "user").orElseThrow();
    User player2 =
        userRepository.findUserByUsername("conflictp2" + suffix + "user").orElseThrow();
    User player3 =
        userRepository.findUserByUsername("conflictp3" + suffix + "user").orElseThrow();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Conflict Test", null, TournamentFormat.SINGLE_ELIMINATION))))
            .andExpect(status().isCreated())
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.generateBracket(tournamentId, organizer);
    tournamentService.startTournament(tournamentId, organizer);

    UUID matchId =
        matchRepository
            .findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(tournamentId)
            .stream()
            .filter(match -> !match.isBye())
            .findFirst()
            .orElseThrow()
            .getId();

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk());

    MvcResult detailResult =
        mockMvc
            .perform(
                get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
            .andExpect(status().isOk())
            .andReturn();

    var detailJson = objectMapper.readTree(detailResult.getResponse().getContentAsString());
    UUID teamOneId = UUID.fromString(detailJson.get("teamOne").get("id").asText());
    UUID teamTwoId = UUID.fromString(detailJson.get("teamTwo").get("id").asText());

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new SubmitTournamentGameResultRequest(teamOneId, 21, 15))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new SubmitTournamentGameResultRequest(teamTwoId, 18, 21))))
        .andExpect(status().isConflict());

    Match persistedMatch = matchRepository.findById(matchId).orElseThrow();
    assertThat(persistedMatch.getTeamOneWins()).isEqualTo(1);
    assertThat(persistedMatch.getTeamTwoWins()).isEqualTo(0);
  }

  @Test
  void submitResult_whenBestOfOneMatchCompletes_matchCompletes() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("org" + suffix);
    registerAndGetToken("p1" + suffix);
    registerAndGetToken("p2" + suffix);
    registerAndGetToken("p3" + suffix);

    User player1 = userRepository.findUserByUsername("p1" + suffix + "user").orElseThrow();
    User player2 = userRepository.findUserByUsername("p2" + suffix + "user").orElseThrow();
    User player3 = userRepository.findUserByUsername("p3" + suffix + "user").orElseThrow();

    // Create tournament via HTTP to get the join code
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Test", null, TournamentFormat.SINGLE_ELIMINATION))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    // Set up tournament state via services (join/bracket/start endpoints not yet exposed via
    // HTTP)
    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.generateBracket(
        tournamentId, userRepository.findUserByUsername("org" + suffix + "user").orElseThrow());
    tournamentService.startTournament(
        tournamentId, userRepository.findUserByUsername("org" + suffix + "user").orElseThrow());

    // Navigate to the non-bye round-1 match
    List<Match> matches =
        matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(
            tournamentId);
    UUID matchId = matches.stream().filter(m -> !m.isBye()).findFirst().orElseThrow().getId();

    completeMatchWithTeamOneWin(tournamentId, matchId, orgToken);

    mockMvc
        .perform(
            get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.teamOneWins").value(1))
        .andExpect(jsonPath("$.results").isNotEmpty());
  }

  @Test
  void submitResult_whenBestOfThreeFirstGameCompletes_nextGameNumberIncrements()
      throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("bo3org" + suffix);
    registerAndGetToken("bo3p1" + suffix);
    registerAndGetToken("bo3p2" + suffix);
    registerAndGetToken("bo3p3" + suffix);

    User player1 = userRepository.findUserByUsername("bo3p1" + suffix + "user").orElseThrow();
    User player2 = userRepository.findUserByUsername("bo3p2" + suffix + "user").orElseThrow();
    User player3 = userRepository.findUserByUsername("bo3p3" + suffix + "user").orElseThrow();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Bo3 Test", null, TournamentFormat.SINGLE_ELIMINATION))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.generateBracket(
        tournamentId, userRepository.findUserByUsername("bo3org" + suffix + "user").orElseThrow());
    tournamentService.updateRoundSettings(
        tournamentId,
        userRepository.findUserByUsername("bo3org" + suffix + "user").orElseThrow(),
        1,
        3);
    tournamentService.startTournament(
        tournamentId, userRepository.findUserByUsername("bo3org" + suffix + "user").orElseThrow());

    List<Match> matches =
        matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(
            tournamentId);
    UUID matchId = matches.stream().filter(m -> !m.isBye()).findFirst().orElseThrow().getId();

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk());

    submitNextGameResult(tournamentId, matchId, orgToken, true);

    mockMvc
        .perform(
            get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.teamOneWins").value(1))
        .andExpect(jsonPath("$.nextGameNumber").value(2))
        .andExpect(jsonPath("$.results").isNotEmpty());

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nextGameNumber").value(2));
  }

  private void completeMatchWithTeamOneWin(UUID tournamentId, UUID matchId, String orgToken)
      throws Exception {
    completeMatch(tournamentId, matchId, orgToken, true);
  }

  private void completeMatchWithTeamTwoWin(UUID tournamentId, UUID matchId, String orgToken)
      throws Exception {
    completeMatch(tournamentId, matchId, orgToken, false);
  }

  private void completeDoublesMatch(
      UUID tournamentId, UUID matchId, String orgToken, boolean teamOneWins) throws Exception {
    completeMatch(tournamentId, matchId, orgToken, teamOneWins);
  }

  private void completeMatch(
      UUID tournamentId, UUID matchId, String orgToken, boolean teamOneWins) throws Exception {
    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk());

    while (true) {
      MvcResult detailResult =
          mockMvc
              .perform(
                  get(
                          "/api/v1/tournaments/{tournamentId}/matches/{matchId}",
                          tournamentId,
                          matchId)
                      .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
              .andExpect(status().isOk())
              .andReturn();

      var detailJson = objectMapper.readTree(detailResult.getResponse().getContentAsString());
      if ("COMPLETED".equals(detailJson.get("status").asText())) {
        break;
      }
      submitNextGameResult(tournamentId, matchId, orgToken, teamOneWins, detailJson);
    }
  }

  private void submitNextGameResult(
      UUID tournamentId, UUID matchId, String orgToken, boolean teamOneWins) throws Exception {
    MvcResult detailResult =
        mockMvc
            .perform(
                get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
            .andExpect(status().isOk())
            .andReturn();
    submitNextGameResult(
        tournamentId, matchId, orgToken, teamOneWins,
        objectMapper.readTree(detailResult.getResponse().getContentAsString()));
  }

  private void submitNextGameResult(
      UUID tournamentId,
      UUID matchId,
      String orgToken,
      boolean teamOneWins,
      com.fasterxml.jackson.databind.JsonNode detailJson)
      throws Exception {
    int gameNumber = detailJson.get("nextGameNumber").asInt();
    UUID teamOneId = UUID.fromString(detailJson.get("teamOne").get("id").asText());
    UUID teamTwoId = UUID.fromString(detailJson.get("teamTwo").get("id").asText());
    UUID winnerTeamId = teamOneWins ? teamOneId : teamTwoId;
    int teamOneScore = teamOneWins ? 21 : 15;
    int teamTwoScore = teamOneWins ? 15 : 21;

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    gameNumber)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new SubmitTournamentGameResultRequest(
                            winnerTeamId, teamOneScore, teamTwoScore))))
        .andExpect(status().isCreated());
  }

  private List<Match> tournamentMatches(UUID tournamentId) {
    return matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(
        tournamentId);
  }

  private Match match(
      List<Match> matches, BracketType bracketType, int roundNumber, int matchNumber) {
    return matches.stream()
        .filter(candidate -> candidate.getRound().getBracketType() == bracketType)
        .filter(candidate -> candidate.getRound().getRoundNumber() == roundNumber)
        .filter(candidate -> candidate.getMatchNumber() == matchNumber)
        .findFirst()
        .orElseThrow();
  }

  private TournamentTeam team(List<Match> matches, UUID teamId) {
    return matches.stream()
        .flatMap(
            candidate ->
                java.util.stream.Stream.of(candidate.getTeamOne(), candidate.getTeamTwo()))
        .filter(java.util.Objects::nonNull)
        .filter(candidate -> candidate.getId().equals(teamId))
        .findFirst()
        .orElseThrow();
  }
}
