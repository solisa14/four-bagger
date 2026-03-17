package com.github.solisa14.fourbagger.api.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestCookieHelper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class GameFlowIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void gameFlow_whenPlayedToTargetScore_completesWithWinner() throws Exception {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    String p1Token = registerAndGetToken("p1" + suffix);
    String p2Token = registerAndGetToken("p2" + suffix);

    // Get player 2's ID
    MvcResult p2Profile =
        mockMvc
            .perform(get("/api/v1/user/me").cookie(TestCookieHelper.cookie("accessToken", p2Token)))
            .andExpect(status().isOk())
            .andReturn();

    String p2Id =
        objectMapper.readTree(p2Profile.getResponse().getContentAsString()).get("id").asText();

    // Create game (player 1 creates)
    CreateGameRequest createRequest = new CreateGameRequest(java.util.UUID.fromString(p2Id), 21);
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/games")
                    .cookie(TestCookieHelper.cookie("accessToken", p1Token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();

    String gameId =
        objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

    // Start game
    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/start", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", p1Token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    // Record frames until player 1 wins (7 frames × 3pts = 21)
    RecordFrameRequest fourBagger = new RecordFrameRequest(4, 0, 0, 0); // p1 nets 12
    RecordFrameRequest threePoints = new RecordFrameRequest(1, 0, 0, 0); // p1 nets 3

    // Frame 1: p1 four-bagger = 12pts
    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/frames", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", p1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fourBagger)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.playerOneScore").value(12));

    // Frames 2-4: p1 gets 3 each = 9 more (total 21)
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              post("/api/v1/games/{gameId}/frames", gameId)
                  .cookie(TestCookieHelper.cookie("accessToken", p1Token))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(threePoints)))
          .andExpect(status().isCreated());
    }

    // Verify final state
    MvcResult finalState =
        mockMvc
            .perform(
                get("/api/v1/games/{gameId}", gameId)
                    .cookie(TestCookieHelper.cookie("accessToken", p1Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andReturn();

    var finalGame = objectMapper.readTree(finalState.getResponse().getContentAsString());
    assertThat(finalGame.get("playerOneScore").asInt()).isEqualTo(21);
    assertThat(finalGame.get("winner").get("id").asText()).isNotBlank();
    assertThat(finalGame.get("frames").size()).isEqualTo(4);
  }

  @Test
  void cancelGame_whenRequested_setsStatusToCancelled() throws Exception {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    String p1Token = registerAndGetToken("cancel1" + suffix);
    String p2Token = registerAndGetToken("cancel2" + suffix);

    MvcResult p2Profile =
        mockMvc
            .perform(get("/api/v1/user/me").cookie(TestCookieHelper.cookie("accessToken", p2Token)))
            .andExpect(status().isOk())
            .andReturn();
    String p2Id =
        objectMapper.readTree(p2Profile.getResponse().getContentAsString()).get("id").asText();

    CreateGameRequest createRequest = new CreateGameRequest(java.util.UUID.fromString(p2Id), null);
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/games")
                    .cookie(TestCookieHelper.cookie("accessToken", p1Token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    String gameId =
        objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/cancel", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", p1Token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void gameMutations_whenUserIsNotParticipantOrCreator_returnForbidden() throws Exception {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    String p1Token = registerAndGetToken("authz1" + suffix);
    String p2Token = registerAndGetToken("authz2" + suffix);
    String outsiderToken = registerAndGetToken("authz3" + suffix);

    MvcResult p2Profile =
        mockMvc
            .perform(get("/api/v1/user/me").cookie(TestCookieHelper.cookie("accessToken", p2Token)))
            .andExpect(status().isOk())
            .andReturn();
    String p2Id =
        objectMapper.readTree(p2Profile.getResponse().getContentAsString()).get("id").asText();

    CreateGameRequest createRequest = new CreateGameRequest(java.util.UUID.fromString(p2Id), null);
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/games")
                    .cookie(TestCookieHelper.cookie("accessToken", p1Token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    String gameId =
        objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/start", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/start", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", p1Token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/frames", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RecordFrameRequest(1, 0, 0, 0))))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/cancel", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken)))
        .andExpect(status().isForbidden());
  }

  @Test
  void listMyGames_whenUserHasGames_returnsCurrentUsersGames() throws Exception {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    String p1Token = registerAndGetToken("list1" + suffix);
    String p2Token = registerAndGetToken("list2" + suffix);

    MvcResult p2Profile =
        mockMvc
            .perform(get("/api/v1/user/me").cookie(TestCookieHelper.cookie("accessToken", p2Token)))
            .andExpect(status().isOk())
            .andReturn();
    String p2Id =
        objectMapper.readTree(p2Profile.getResponse().getContentAsString()).get("id").asText();

    CreateGameRequest createRequest = new CreateGameRequest(java.util.UUID.fromString(p2Id), null);

    // Create two games
    for (int i = 0; i < 2; i++) {
      mockMvc
          .perform(
              post("/api/v1/games")
                  .cookie(TestCookieHelper.cookie("accessToken", p1Token))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(createRequest)))
          .andExpect(status().isCreated());
    }

    mockMvc
        .perform(get("/api/v1/games/me").cookie(TestCookieHelper.cookie("accessToken", p1Token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }
}
