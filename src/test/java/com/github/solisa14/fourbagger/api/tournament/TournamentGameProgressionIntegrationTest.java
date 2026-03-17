package com.github.solisa14.fourbagger.api.tournament;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.game.RecordFrameRequest;
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
 * Integration tests verifying that completing a tournament-backed game automatically triggers match
 * progression — no separate HTTP call required.
 */
class TournamentGameProgressionIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private TournamentService tournamentService;
  @Autowired private MatchRepository matchRepository;
  @Autowired private UserRepository userRepository;

  @Test
  void recordFrame_whenTournamentGameCompletes_matchAutoCompletes() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    // Organizer creates the tournament; tournament games are created with organizer as createdBy,
    // so the organizer token authorizes all game mutations regardless of seeding randomness.
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
                        objectMapper.writeValueAsString(new CreateTournamentRequest("Test", null))))
            .andExpect(status().isCreated())
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    // Set up tournament state via services (join/bracket/start endpoints not yet exposed via HTTP)
    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.generateBracket(tournamentId);
    tournamentService.startTournament(tournamentId);

    // Navigate to the non-bye round-1 match
    List<Match> matches =
        matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(
            tournamentId);
    UUID matchId = matches.stream().filter(m -> !m.isBye()).findFirst().orElseThrow().getId();

    // Start match via HTTP — lazily creates first game
    MvcResult startMatchResult =
        mockMvc
            .perform(
                post(
                        "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                        tournamentId,
                        matchId)
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
            .andExpect(status().isOk())
            .andReturn();

    String gameId =
        objectMapper
            .readTree(startMatchResult.getResponse().getContentAsString())
            .get("id")
            .asText();

    // Start game
    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/start", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk());

    // Record frames until team one wins: 4-bagger (12 pts) + three 1-bag frames (9 pts) = 21
    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/frames", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RecordFrameRequest(4, 0, 0, 0))))
        .andExpect(status().isCreated());

    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              post("/api/v1/games/{gameId}/frames", gameId)
                  .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new RecordFrameRequest(1, 0, 0, 0))))
          .andExpect(status().isCreated());
    }

    // Match should be auto-completed with teamOneWins=1 — no manual /complete-game call
    mockMvc
        .perform(
            get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.teamOneWins").value(1));
  }

  @Test
  void recordFrame_whenBestOfThreeFirstGameCompletes_nextGameCreatedAutomatically()
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
                            new CreateTournamentRequest("Bo3 Test", null))))
            .andExpect(status().isCreated())
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    tournamentService.joinTournament(joinCode, player1);
    tournamentService.joinTournament(joinCode, player2);
    tournamentService.joinTournament(joinCode, player3);
    tournamentService.generateBracket(tournamentId);
    tournamentService.updateRoundSettings(tournamentId, 1, 3, ScoringMode.STANDARD);
    tournamentService.startTournament(tournamentId);

    List<Match> matches =
        matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(
            tournamentId);
    UUID matchId = matches.stream().filter(m -> !m.isBye()).findFirst().orElseThrow().getId();

    // Start the match and get game 1
    MvcResult startMatchResult =
        mockMvc
            .perform(
                post(
                        "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                        tournamentId,
                        matchId)
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
            .andExpect(status().isOk())
            .andReturn();

    String game1Id =
        objectMapper
            .readTree(startMatchResult.getResponse().getContentAsString())
            .get("id")
            .asText();

    // Start and complete game 1
    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/start", game1Id)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/frames", game1Id)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RecordFrameRequest(4, 0, 0, 0))))
        .andExpect(status().isCreated());

    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              post("/api/v1/games/{gameId}/frames", game1Id)
                  .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new RecordFrameRequest(1, 0, 0, 0))))
          .andExpect(status().isCreated());
    }

    // Match should still be IN_PROGRESS (series not yet decided)
    mockMvc
        .perform(
            get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.teamOneWins").value(1));

    // Start the match again — should return game 2 (auto-created by progression)
    MvcResult startMatch2Result =
        mockMvc
            .perform(
                post(
                        "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                        tournamentId,
                        matchId)
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
            .andExpect(status().isOk())
            .andReturn();

    String game2Id =
        objectMapper
            .readTree(startMatch2Result.getResponse().getContentAsString())
            .get("id")
            .asText();

    // Game 2 must be a different entity from game 1
    assert !game2Id.equals(game1Id);
  }
}
