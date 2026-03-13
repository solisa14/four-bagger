package com.github.solisa14.fourbagger.api.tournament;

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

@WebMvcTest(TournamentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TournamentControllerWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private MockMvc mockMvc;
  @MockitoBean private TournamentService tournamentService;
  @MockitoBean private com.github.solisa14.fourbagger.api.security.JwtService jwtService;

  private User authenticatedUser() {
    return TestDataFactory.user(
        UUID.randomUUID(), "testuser", "test@example.com", "encoded", Role.USER);
  }

  @Test
  void createTournament_whenValidRequest_returnsCreated() throws Exception {
    User principal = authenticatedUser();
    Tournament tournament =
        TestDataFactory.tournament(UUID.randomUUID(), principal, "TestTournament", "ABC123");
    when(tournamentService.createTournament(any(), eq("TestTournament"))).thenReturn(tournament);

    mockMvc
        .perform(
            post("/api/v1/tournaments")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new CreateTournamentRequest("TestTournament"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("TestTournament"))
        .andExpect(jsonPath("$.joinCode").value("ABC123"))
        .andExpect(jsonPath("$.status").value("REGISTRATION"));
  }

  // TODO(human): Write two tests for GET /api/v1/tournaments/{id}:
  //
  //   Test 1 — getTournament_whenNotFound_returnsNotFound
  //     - Stub: when(tournamentService.getTournament(id)).thenThrow(new
  // TournamentNotFoundException())
  //     - Assert: status 404, jsonPath("$.message").value("Tournament not found")
  //
  //   Test 2 — getTournament_whenFound_returnsOk
  //     - Build a tournament with TestDataFactory, stub getTournament to return it
  //     - Assert: status 200, jsonPath("$.title") and jsonPath("$.status") match
  //
  // Note: GET requests don't have a body — just .perform(get(...).with(user(principal))).
  @Test
  void getTournament_whenNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    when(tournamentService.getTournament(any())).thenThrow(new TournamentNotFoundException());
    mockMvc
        .perform(get("/api/v1/tournaments/{id}", UUID.randomUUID()).with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void getTournament_whenFound_returnsOk() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    Tournament tournament = TestDataFactory.tournament(id, principal, "TestTournament", "ABC123");
    when(tournamentService.getTournament(any())).thenReturn(tournament);
    mockMvc
        .perform(get("/api/v1/tournaments/{id}", id).with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.joinCode").value("ABC123"))
        .andExpect(jsonPath("$.title").value("TestTournament"))
        .andExpect(jsonPath("$.status").value("REGISTRATION"));
  }

  @Test
  void createTournament_whenTitleMissing_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    String body = "{}";

    mockMvc
        .perform(
            post("/api/v1/tournaments")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }
}
