package com.github.solisa14.fourbagger.api.tournament;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestCookieHelper;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests verifying that {@code GET /api/v1/tournaments/{id}} returns the correct nested
 * bracket structure at different points in the tournament lifecycle.
 */
class TournamentReadIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private TournamentService tournamentService;
  @Autowired private UserRepository userRepository;

  @Test
  void getTournament_beforeBracketGenerated_returnsEmptyRounds() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("readorg" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest("Read Test", null))))
            .andExpect(status().isCreated())
            .andReturn();

    UUID tournamentId =
        UUID.fromString(
            objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText());

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tournamentId.toString()))
        .andExpect(jsonPath("$.title").value("Read Test"))
        .andExpect(jsonPath("$.status").value("REGISTRATION"))
        .andExpect(jsonPath("$.rounds").isArray())
        .andExpect(jsonPath("$.rounds").isEmpty());
  }

  @Test
  void getTournament_afterTournamentStarted_returnsNestedRoundsWithMatches() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("readorg2" + suffix);
    registerAndGetToken("readp1" + suffix);
    registerAndGetToken("readp2" + suffix);
    registerAndGetToken("readp3" + suffix);

    User organizer = userRepository.findUserByUsername("readorg2" + suffix + "user").orElseThrow();
    User player1 = userRepository.findUserByUsername("readp1" + suffix + "user").orElseThrow();
    User player2 = userRepository.findUserByUsername("readp2" + suffix + "user").orElseThrow();
    User player3 = userRepository.findUserByUsername("readp3" + suffix + "user").orElseThrow();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest("Read Bracket Test", null))))
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

    // With 3 participants: 2 rounds (round 1 has 2 matches — 1 bye + 1 real; round 2 has 1 final)
    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.rounds").isArray())
        .andExpect(jsonPath("$.rounds.length()").value(2))
        .andExpect(jsonPath("$.rounds[0].roundNumber").value(1))
        .andExpect(jsonPath("$.rounds[0].bestOf").value(1))
        .andExpect(jsonPath("$.rounds[0].scoringMode").value("STANDARD"))
        .andExpect(jsonPath("$.rounds[0].matches").isArray())
        .andExpect(jsonPath("$.rounds[0].matches.length()").value(2))
        .andExpect(jsonPath("$.rounds[1].roundNumber").value(2))
        .andExpect(jsonPath("$.rounds[1].matches.length()").value(1));
  }
}
