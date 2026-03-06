package com.github.solisa14.fourbagger.api.game;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GameControllerWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private MockMvc mockMvc;
  @MockitoBean private GameService gameService;
  @MockitoBean private com.github.solisa14.fourbagger.api.security.JwtService jwtService;

  private User authenticatedUser() {
    User user =
        TestDataFactory.user(UUID.randomUUID(), "testuser", "test@example.com", "encoded", Role.USER);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    return user;
  }

  @Test
  void createGame_returnsBadRequestWhenPlayerTwoIdMissing() throws Exception {
    authenticatedUser();
    String body = "{}";

    mockMvc
        .perform(post("/api/v1/games").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createGame_returnsBadRequestWhenTargetScoreTooLow() throws Exception {
    authenticatedUser();
    String body =
        objectMapper.writeValueAsString(
            new CreateGameRequest(UUID.randomUUID(), 5, null));

    mockMvc
        .perform(post("/api/v1/games").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void recordFrame_returnsBadRequestWhenBagsOutOfRange() throws Exception {
    authenticatedUser();
    UUID gameId = UUID.randomUUID();
    // p1BagsIn = 5, which violates @Max(4)
    String body = objectMapper.writeValueAsString(new RecordFrameRequest(5, 0, 0, 0));

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/frames", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getGame_returns404WhenGameNotFound() throws Exception {
    authenticatedUser();
    UUID gameId = UUID.randomUUID();
    when(gameService.getGame(gameId)).thenThrow(new GameNotFoundException(gameId));

    mockMvc
        .perform(get("/api/v1/games/{gameId}", gameId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Game not found: " + gameId));
  }

  @Test
  void recordFrame_returns400WhenGameNotInProgress() throws Exception {
    authenticatedUser();
    UUID gameId = UUID.randomUUID();
    when(gameService.recordFrame(eq(gameId), any(RecordFrameRequest.class)))
        .thenThrow(new InvalidGameStateException("Cannot record a frame for a game that is not IN_PROGRESS"));

    String body = objectMapper.writeValueAsString(new RecordFrameRequest(1, 0, 0, 0));

    mockMvc
        .perform(
            post("/api/v1/games/{gameId}/frames", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }
}
