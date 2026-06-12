package com.github.solisa14.fourbagger.api.game;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.common.exception.GlobalExceptionHandler;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GameMapper.class})
class GameControllerWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private MockMvc mockMvc;
  @MockitoBean private GameService gameService;
  @MockitoBean private com.github.solisa14.fourbagger.api.security.JwtService jwtService;
  @MockitoBean private com.github.solisa14.fourbagger.api.user.UserService userService;

  private User authenticatedUser() {
    return TestDataFactory.user(
        UUID.randomUUID(), "testuser", "encoded", Role.USER);
  }

  @Test
  void createGame_whenPlayerTwoIdMissing_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    String body = "{}";

    mockMvc
        .perform(
            post("/api/v1/games")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitResult_whenWinnerUserIdMissing_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID gameId = UUID.randomUUID();
    String body = "{\"playerOneScore\":21,\"playerTwoScore\":15}";

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/result", gameId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitResult_whenPlayerOneScoreMissing_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID gameId = UUID.randomUUID();
    String body =
        "{\"winnerUserId\":\""
            + principal.getId()
            + "\",\"playerTwoScore\":15}";

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/result", gameId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitResult_whenPlayerTwoScoreMissing_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID gameId = UUID.randomUUID();
    String body =
        "{\"winnerUserId\":\""
            + principal.getId()
            + "\",\"playerOneScore\":21}";

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/result", gameId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitResult_whenScoreIsNegative_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID gameId = UUID.randomUUID();
    String body =
        objectMapper.writeValueAsString(
            new SubmitGameResultRequest(UUID.randomUUID(), -1, 15));

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/result", gameId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getGame_whenGameNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID gameId = UUID.randomUUID();
    when(gameService.getGameForUser(org.mockito.ArgumentMatchers.nullable(User.class), eq(gameId)))
        .thenThrow(new GameNotFoundException(gameId));

    mockMvc
        .perform(get("/api/v1/games/{gameId}", gameId).with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Game not found: " + gameId));
  }

  @Test
  void submitResult_whenGameNotInProgress_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID gameId = UUID.randomUUID();
    when(gameService.submitResult(
            org.mockito.ArgumentMatchers.nullable(User.class),
            eq(gameId),
            any(SubmitGameResultRequest.class)))
        .thenThrow(
            new InvalidGameStateException(
                "Cannot submit a result for a game that is not IN_PROGRESS. Current status: PENDING"));

    String body =
        objectMapper.writeValueAsString(
            new SubmitGameResultRequest(principal.getId(), 21, 15));

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/result", gameId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitResult_whenResultAlreadySubmitted_returnsConflict() throws Exception {
    User principal = authenticatedUser();
    UUID gameId = UUID.randomUUID();
    when(gameService.submitResult(
            org.mockito.ArgumentMatchers.nullable(User.class),
            eq(gameId),
            any(SubmitGameResultRequest.class)))
        .thenThrow(new ResultAlreadySubmittedException());

    String body =
        objectMapper.writeValueAsString(
            new SubmitGameResultRequest(principal.getId(), 21, 15));

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/result", gameId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Game result has already been submitted"));
  }

  @Test
  void cancelGame_whenUserCannotModifyGame_returnsForbidden() throws Exception {
    User principal = authenticatedUser();
    UUID gameId = UUID.randomUUID();
    when(gameService.cancelGame(org.mockito.ArgumentMatchers.nullable(User.class), eq(gameId)))
        .thenThrow(new GameAccessDeniedException());

    mockMvc
        .perform(post("/api/v1/games/{gameId}/cancel", gameId).with(user(principal)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("You are not allowed to access this game"));
  }
}
