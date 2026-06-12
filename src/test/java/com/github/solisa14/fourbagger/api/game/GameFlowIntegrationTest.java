package com.github.solisa14.fourbagger.api.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestCookieHelper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class GameFlowIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void gameFlow_whenResultSubmitted_completesWithWinner() throws Exception {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    String p1Token = registerAndGetToken("p1" + suffix);
    String p2Token = registerAndGetToken("p2" + suffix);

    MvcResult p1Profile =
        mockMvc
            .perform(get("/api/v1/user/me").cookie(TestCookieHelper.cookie("accessToken", p1Token)))
            .andExpect(status().isOk())
            .andReturn();
    MvcResult p2Profile =
        mockMvc
            .perform(get("/api/v1/user/me").cookie(TestCookieHelper.cookie("accessToken", p2Token)))
            .andExpect(status().isOk())
            .andReturn();

    String p1Id =
        objectMapper.readTree(p1Profile.getResponse().getContentAsString()).get("id").asText();
    String p2Id =
        objectMapper.readTree(p2Profile.getResponse().getContentAsString()).get("id").asText();

    CreateGameRequest createRequest =
        new CreateGameRequest(java.util.UUID.fromString(p2Id));
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

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/start", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", p1Token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    SubmitGameResultRequest resultRequest =
        new SubmitGameResultRequest(
            java.util.UUID.fromString(p1Id), 21, 15);

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/result", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", p1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resultRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.playerOneScore").value(21))
        .andExpect(jsonPath("$.playerTwoScore").value(15))
        .andExpect(jsonPath("$.winner.id").value(p1Id))
        .andExpect(jsonPath("$.submittedBy.id").value(p1Id))
        .andExpect(jsonPath("$.completedAt").exists());

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
    assertThat(finalGame.get("winner").get("id").asText()).isEqualTo(p1Id);
    assertThat(finalGame.has("frames")).isFalse();
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

    CreateGameRequest createRequest =
        new CreateGameRequest(java.util.UUID.fromString(p2Id));
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

    MvcResult p1Profile =
        mockMvc
            .perform(get("/api/v1/user/me").cookie(TestCookieHelper.cookie("accessToken", p1Token)))
            .andExpect(status().isOk())
            .andReturn();
    MvcResult p2Profile =
        mockMvc
            .perform(get("/api/v1/user/me").cookie(TestCookieHelper.cookie("accessToken", p2Token)))
            .andExpect(status().isOk())
            .andReturn();
    String p1Id =
        objectMapper.readTree(p1Profile.getResponse().getContentAsString()).get("id").asText();
    String p2Id =
        objectMapper.readTree(p2Profile.getResponse().getContentAsString()).get("id").asText();

    CreateGameRequest createRequest =
        new CreateGameRequest(java.util.UUID.fromString(p2Id));
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

    SubmitGameResultRequest resultRequest =
        new SubmitGameResultRequest(
            java.util.UUID.fromString(p1Id), 21, 15);

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/result", gameId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resultRequest)))
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

    CreateGameRequest createRequest =
        new CreateGameRequest(java.util.UUID.fromString(p2Id));

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
