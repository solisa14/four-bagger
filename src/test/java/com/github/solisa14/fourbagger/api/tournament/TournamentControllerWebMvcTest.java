package com.github.solisa14.fourbagger.api.tournament;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.common.exception.GlobalExceptionHandler;
import com.github.solisa14.fourbagger.api.game.GameType;
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

/**
 * WebMvc tests for {@link TournamentController}. Validates HTTP status codes, response body
 * structure, and exception-to-status mapping for all tournament lifecycle endpoints.
 */
@WebMvcTest(TournamentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TournamentMapper.class})
class TournamentControllerWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private MockMvc mockMvc;
  @MockitoBean private TournamentService tournamentService;
  @MockitoBean private com.github.solisa14.fourbagger.api.security.JwtService jwtService;

  private User authenticatedUser() {
    return TestDataFactory.user(
        UUID.randomUUID(), "testuser", "test@example.com", "encoded", Role.USER);
  }

  private Tournament registrationTournament(UUID id, User organizer) {
    return TestDataFactory.tournament(id, organizer, "TestTournament", "ABC123");
  }

  private Tournament bracketReadyTournament(UUID id, User organizer) {
    return Tournament.builder()
        .id(id)
        .organizer(organizer)
        .title("TestTournament")
        .joinCode("ABC123")
        .status(TournamentStatus.BRACKET_READY)
        .build();
  }

  private Tournament inProgressTournament(UUID id, User organizer) {
    return Tournament.builder()
        .id(id)
        .organizer(organizer)
        .title("TestTournament")
        .joinCode("ABC123")
        .status(TournamentStatus.IN_PROGRESS)
        .build();
  }

  // ── Create Tournament ─────────────────────────────────────────

  @Test
  void createTournament_whenValidRequest_returnsCreated() throws Exception {
    User principal = authenticatedUser();
    Tournament tournament = registrationTournament(UUID.randomUUID(), principal);
    when(tournamentService.createTournament(any(CreateTournamentCommand.class)))
        .thenReturn(tournament);

    mockMvc
        .perform(
            post("/api/v1/tournaments")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreateTournamentRequest("TestTournament", null))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("TestTournament"))
        .andExpect(jsonPath("$.joinCode").value("ABC123"))
        .andExpect(jsonPath("$.status").value("REGISTRATION"))
        .andExpect(jsonPath("$.gameType").value("SINGLES"))
        .andExpect(jsonPath("$.rounds").isArray());
  }

  @Test
  void createTournament_whenDoublesType_returnsCreatedWithDoubles() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    Tournament tournament =
        TestDataFactory.tournament(id, principal, "TestTournament", "ABC123", GameType.DOUBLES);
    when(tournamentService.createTournament(any(CreateTournamentCommand.class)))
        .thenReturn(tournament);

    mockMvc
        .perform(
            post("/api/v1/tournaments")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreateTournamentRequest("TestTournament", GameType.DOUBLES))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.gameType").value("DOUBLES"));
  }

  @Test
  void createTournament_whenNoGameType_defaultsToSingles() throws Exception {
    User principal = authenticatedUser();
    Tournament tournament = registrationTournament(UUID.randomUUID(), principal);
    when(tournamentService.createTournament(any(CreateTournamentCommand.class)))
        .thenReturn(tournament);

    mockMvc
        .perform(
            post("/api/v1/tournaments")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreateTournamentRequest("TestTournament", null))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.gameType").value("SINGLES"));
  }

  @Test
  void createTournament_whenTitleMissing_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();

    mockMvc
        .perform(
            post("/api/v1/tournaments")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  // ── Get Tournament ────────────────────────────────────────────

  @Test
  void getTournament_whenFound_returnsOk() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    Tournament tournament = registrationTournament(id, principal);
    when(tournamentService.getTournament(any())).thenReturn(tournament);

    mockMvc
        .perform(get("/api/v1/tournaments/{id}", id).with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.joinCode").value("ABC123"))
        .andExpect(jsonPath("$.title").value("TestTournament"))
        .andExpect(jsonPath("$.status").value("REGISTRATION"))
        .andExpect(jsonPath("$.rounds").isArray());
  }

  @Test
  void getTournament_whenNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    when(tournamentService.getTournament(any())).thenThrow(new TournamentNotFoundException());

    mockMvc
        .perform(get("/api/v1/tournaments/{id}", UUID.randomUUID()).with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  // ── Delete Tournament ─────────────────────────────────────────

  @Test
  void deleteTournament_whenFound_returnsNoContent() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doNothing().when(tournamentService).deleteTournament(id);

    mockMvc
        .perform(delete("/api/v1/tournaments/{id}", id).with(user(principal)))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteTournament_whenNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentNotFoundException()).when(tournamentService).deleteTournament(id);

    mockMvc
        .perform(delete("/api/v1/tournaments/{id}", id).with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  // ── Start Tournament ──────────────────────────────────────────

  @Test
  void startTournament_whenBracketReady_returnsOkWithTournament() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doNothing().when(tournamentService).startTournament(id);
    when(tournamentService.getTournament(id)).thenReturn(inProgressTournament(id, principal));

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/start", id).with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  void startTournament_whenNotBracketReady_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(
            new InvalidTournamentStateException(
                "Tournament can only be started when bracket is ready"))
        .when(tournamentService)
        .startTournament(id);

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/start", id).with(user(principal)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("Tournament can only be started when bracket is ready"));
  }

  @Test
  void startTournament_whenNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentNotFoundException()).when(tournamentService).startTournament(id);

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/start", id).with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  // ── Generate Bracket ──────────────────────────────────────────

  @Test
  void generateBracket_whenRegistration_returnsOkWithTournament() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doNothing().when(tournamentService).generateBracket(id);
    when(tournamentService.getTournament(id)).thenReturn(bracketReadyTournament(id, principal));

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/bracket", id).with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value("BRACKET_READY"));
  }

  @Test
  void generateBracket_whenInvalidState_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(
            new InvalidTournamentStateException(
                "Cannot generate or reshuffle bracket unless tournament is in REGISTRATION or BRACKET_READY"))
        .when(tournamentService)
        .generateBracket(id);

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/bracket", id).with(user(principal)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void generateBracket_whenTooFewParticipants_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(
            new InvalidTournamentStateException(
                "Cannot generate bracket with 2 or fewer participants"))
        .when(tournamentService)
        .generateBracket(id);

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/bracket", id).with(user(principal)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("Cannot generate bracket with 2 or fewer participants"));
  }

  @Test
  void generateBracket_whenNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentNotFoundException()).when(tournamentService).generateBracket(id);

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/bracket", id).with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void generateBracket_whenDoubles_returnsOkWithDoublesTournament() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    Tournament tournament =
        Tournament.builder()
            .id(id)
            .organizer(principal)
            .title("TestTournament")
            .joinCode("ABC123")
            .status(TournamentStatus.BRACKET_READY)
            .gameType(GameType.DOUBLES)
            .build();
    doNothing().when(tournamentService).generateBracket(id);
    when(tournamentService.getTournament(id)).thenReturn(tournament);

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/bracket", id).with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value("BRACKET_READY"))
        .andExpect(jsonPath("$.gameType").value("DOUBLES"));
  }

  // ── Join Tournament ───────────────────────────────────────────

  @Test
  void joinTournament_whenValidCode_returnsOkWithTournament() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    Tournament tournament = registrationTournament(tournamentId, principal);
    TournamentParticipant participant =
        TournamentParticipant.builder().tournament(tournament).user(principal).build();

    when(tournamentService.joinTournament(any(), any())).thenReturn(participant);
    when(tournamentService.getTournament(tournamentId)).thenReturn(tournament);

    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest("ABC123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tournamentId.toString()))
        .andExpect(jsonPath("$.status").value("REGISTRATION"));
  }

  @Test
  void joinTournament_whenCodeNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    when(tournamentService.joinTournament(any(), any()))
        .thenThrow(new TournamentNotFoundException());

    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest("BADCODE"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void joinTournament_whenAlreadyJoined_returnsConflict() throws Exception {
    User principal = authenticatedUser();
    when(tournamentService.joinTournament(any(), any()))
        .thenThrow(new DuplicateTournamentParticipantException());

    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest("ABC123"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("User is already registered in this tournament"));
  }

  @Test
  void joinTournament_whenNotInRegistration_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    when(tournamentService.joinTournament(any(), any()))
        .thenThrow(new InvalidTournamentStateException("Tournament is not open for registration"));

    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest("ABC123"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Tournament is not open for registration"));
  }

  @Test
  void joinTournament_whenJoinCodeBlank_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();

    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest(""))))
        .andExpect(status().isBadRequest());
  }

  // ── Remove Participant ────────────────────────────────────────

  @Test
  void removeParticipant_whenValid_returnsNoContent() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    doNothing().when(tournamentService).removeParticipant(tournamentId, participantId);

    mockMvc
        .perform(
            delete(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .with(user(principal)))
        .andExpect(status().isNoContent());
  }

  @Test
  void removeParticipant_whenNotRegistration_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    doThrow(new InvalidTournamentStateException("Cannot remove participants after registration"))
        .when(tournamentService)
        .removeParticipant(tournamentId, participantId);

    mockMvc
        .perform(
            delete(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .with(user(principal)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Cannot remove participants after registration"));
  }

  @Test
  void removeParticipant_whenParticipantNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    doThrow(new TournamentParticipantNotFoundException())
        .when(tournamentService)
        .removeParticipant(tournamentId, participantId);

    mockMvc
        .perform(
            delete(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament participant not found"));
  }

  @Test
  void removeParticipant_whenTournamentNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    doThrow(new TournamentNotFoundException())
        .when(tournamentService)
        .removeParticipant(tournamentId, participantId);

    mockMvc
        .perform(
            delete(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  // ── Update Round Settings ─────────────────────────────────────

  @Test
  void updateRoundSettings_whenValid_returnsOkWithTournament() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doNothing().when(tournamentService).updateRoundSettings(id, 1, 3, ScoringMode.STANDARD);
    when(tournamentService.getTournament(id)).thenReturn(bracketReadyTournament(id, principal));

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UpdateRoundSettingsRequest(3, ScoringMode.STANDARD))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value("BRACKET_READY"));
  }

  @Test
  void updateRoundSettings_whenNotBracketReady_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(
            new InvalidTournamentStateException(
                "Round settings can only be changed when tournament is BRACKET_READY"))
        .when(tournamentService)
        .updateRoundSettings(eq(id), eq(1), eq(3), eq(ScoringMode.STANDARD));

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UpdateRoundSettingsRequest(3, ScoringMode.STANDARD))))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Round settings can only be changed when tournament is BRACKET_READY"));
  }

  @Test
  void updateRoundSettings_whenInvalidBestOf_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new InvalidRoundConfigurationException("bestOf must be one of: 1, 3, 5, or 7"))
        .when(tournamentService)
        .updateRoundSettings(eq(id), eq(1), eq(2), any());

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(2, null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("bestOf must be one of: 1, 3, 5, or 7"));
  }

  @Test
  void updateRoundSettings_whenRoundNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentRoundNotFoundException())
        .when(tournamentService)
        .updateRoundSettings(eq(id), eq(99), any(), any());

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 99)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3, null))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament round not found"));
  }

  @Test
  void updateRoundSettings_whenTournamentNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentNotFoundException())
        .when(tournamentService)
        .updateRoundSettings(eq(id), eq(1), any(), any());

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3, null))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void updateRoundSettings_whenNoFieldsProvided_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new InvalidRoundConfigurationException("At least one round setting must be provided"))
        .when(tournamentService)
        .updateRoundSettings(eq(id), eq(1), eq(null), eq(null));

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(null, null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("At least one round setting must be provided"));
  }
}
