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
import org.junit.jupiter.api.BeforeEach;
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
@Import({GlobalExceptionHandler.class})
class TournamentControllerWebMvcTest {

  private final TournamentMapper detailMapper =
      new TournamentMapper(new TournamentBracketEligibilityPolicy());

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private MockMvc mockMvc;
  @MockitoBean private TournamentService tournamentService;
  @MockitoBean private TournamentMapper tournamentMapper;
  @MockitoBean private com.github.solisa14.fourbagger.api.security.JwtService jwtService;

  @BeforeEach
  void setUpMapperStubs() {
    when(tournamentMapper.toTournamentResponse(any()))
        .thenAnswer(invocation -> detailMapper.toTournamentResponse(invocation.getArgument(0)));
    when(tournamentMapper.toTournamentListResponse(any()))
        .thenAnswer(
            invocation -> detailMapper.toTournamentListResponse(invocation.getArgument(0)));
    when(tournamentMapper.toTournamentSummaryResponse(any()))
        .thenAnswer(
            invocation -> detailMapper.toTournamentSummaryResponse(invocation.getArgument(0)));
    when(tournamentMapper.toCreateCommand(any(), any()))
        .thenAnswer(
            invocation ->
                detailMapper.toCreateCommand(
                    invocation.getArgument(0), invocation.getArgument(1)));
  }

  private void stubTournamentDetailResponse(Tournament tournament, User viewer) {
    when(tournamentMapper.toTournamentDetailResponse(any(), any()))
        .thenAnswer(
            invocation -> detailMapper.toTournamentDetailResponse(tournament, viewer));
  }

  private User authenticatedUser() {
    return TestDataFactory.user(
        UUID.randomUUID(), "testuser", "encoded", Role.USER);
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
        .participationMode(TournamentParticipationMode.SELF_JOIN)
        .status(TournamentStatus.BRACKET_READY)
        .build();
  }

  private Tournament inProgressTournament(UUID id, User organizer) {
    return Tournament.builder()
        .id(id)
        .organizer(organizer)
        .title("TestTournament")
        .joinCode("ABC123")
        .participationMode(TournamentParticipationMode.SELF_JOIN)
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
                        new CreateTournamentRequest("TestTournament", null, null, TournamentParticipationMode.SELF_JOIN))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("TestTournament"))
        .andExpect(jsonPath("$.joinCode").value("ABC123"))
        .andExpect(jsonPath("$.status").value("REGISTRATION"))
        .andExpect(jsonPath("$.gameType").value("SINGLES"))
        .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
        .andExpect(jsonPath("$.brackets.winners").isArray())
        .andExpect(jsonPath("$.brackets.losers").isArray())
        .andExpect(jsonPath("$.brackets.finalRounds").isArray())
        .andExpect(jsonPath("$.brackets.grandFinal").isArray())
        .andExpect(jsonPath("$.rounds").doesNotExist());
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
                        new CreateTournamentRequest(
                            "TestTournament", GameType.DOUBLES, TournamentFormat.SINGLE_ELIMINATION, TournamentParticipationMode.SELF_JOIN))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.gameType").value("DOUBLES"))
        .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"));
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
                        new CreateTournamentRequest("TestTournament", null, null, TournamentParticipationMode.SELF_JOIN))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.gameType").value("SINGLES"))
        .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"));
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

  @Test
  void createTournament_whenParticipationModeMissing_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();

    mockMvc
        .perform(
            post("/api/v1/tournaments")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"TestTournament\"}"))
        .andExpect(status().isBadRequest());
  }

  // ── Get Tournament ────────────────────────────────────────────

  @Test
  void getTournament_whenFound_returnsOk() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    Tournament tournament = registrationTournament(id, principal);
    when(tournamentService.getTournamentForUser(any(), any())).thenReturn(tournament);
    stubTournamentDetailResponse(tournament, principal);

    mockMvc
        .perform(get("/api/v1/tournaments/{id}", id).with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.joinCode").value("ABC123"))
        .andExpect(jsonPath("$.title").value("TestTournament"))
        .andExpect(jsonPath("$.status").value("REGISTRATION"))
        .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
        .andExpect(jsonPath("$.brackets.winners").isArray())
        .andExpect(jsonPath("$.participants").isArray())
        .andExpect(jsonPath("$.bracketEligibility.participantCount").value(0))
        .andExpect(jsonPath("$.bracketEligibility.eligible").value(false))
        .andExpect(jsonPath("$.viewerCapabilities.canManageTournament").value(true))
        .andExpect(jsonPath("$.viewerCapabilities.canGenerateBracket").value(false))
        .andExpect(jsonPath("$.rounds").doesNotExist());
  }

  @Test
  void getTournament_whenOrganizerWithEligibleParticipants_returnsManagementCapabilities()
      throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    Tournament tournament = registrationTournament(id, principal);
    for (int i = 0; i < 3; i++) {
      tournament
          .getParticipants()
          .add(
              TournamentParticipant.builder()
                  .id(UUID.randomUUID())
                  .tournament(tournament)
                  .user(
                      TestDataFactory.user(
                          UUID.randomUUID(), "player" + i, "encoded", Role.USER))
                  .build());
    }
    when(tournamentService.getTournamentForUser(any(), any())).thenReturn(tournament);
    stubTournamentDetailResponse(tournament, principal);

    mockMvc
        .perform(get("/api/v1/tournaments/{id}", id).with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participants.length()").value(3))
        .andExpect(jsonPath("$.bracketEligibility.eligible").value(true))
        .andExpect(jsonPath("$.viewerCapabilities.canGenerateBracket").value(true))
        .andExpect(jsonPath("$.viewerCapabilities.canRemoveParticipants").value(true));
  }

  @Test
  void getTournament_whenParticipant_returnsLimitedCapabilities() throws Exception {
    User principal = authenticatedUser();
    User organizer = TestDataFactory.user(UUID.randomUUID(), "organizer", "encoded", Role.USER);
    UUID id = UUID.randomUUID();
    Tournament tournament = registrationTournament(id, organizer);
    tournament
        .getParticipants()
        .add(
            TournamentParticipant.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .user(principal)
                .build());
    when(tournamentService.getTournamentForUser(any(), any())).thenReturn(tournament);
    stubTournamentDetailResponse(tournament, principal);

    mockMvc
        .perform(get("/api/v1/tournaments/{id}", id).with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participants[0].currentViewer").value(true))
        .andExpect(jsonPath("$.viewerCapabilities.canManageTournament").value(false))
        .andExpect(jsonPath("$.viewerCapabilities.canGenerateBracket").value(false))
        .andExpect(jsonPath("$.viewerCapabilities.canLeaveRegistration").value(true));
  }

  @Test
  void getTournament_whenNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    when(tournamentService.getTournamentForUser(any(), any()))
        .thenThrow(new TournamentNotFoundException());

    mockMvc
        .perform(get("/api/v1/tournaments/{id}", UUID.randomUUID()).with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void getTournament_whenUserCannotAccess_returnsForbidden() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    when(tournamentService.getTournamentForUser(any(), any()))
        .thenThrow(new TournamentAccessDeniedException(id));

    mockMvc
        .perform(get("/api/v1/tournaments/{id}", id).with(user(principal)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("You are not allowed to modify tournament: " + id));
  }

  // ── Get Tournament By Join Code ──────────────────────────────────

  @Test
  void getTournamentByJoinCode_whenFound_returnsOk() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    Tournament tournament = registrationTournament(id, principal);
    when(tournamentService.getTournamentByJoinCode("ABC123")).thenReturn(tournament);
    stubTournamentDetailResponse(tournament, principal);

    mockMvc
        .perform(get("/api/v1/tournaments/join-code/ABC123").with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.joinCode").value("ABC123"))
        .andExpect(jsonPath("$.title").value("TestTournament"))
        .andExpect(jsonPath("$.status").value("REGISTRATION"));
  }

  @Test
  void getTournamentByJoinCode_whenNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    when(tournamentService.getTournamentByJoinCode("BADCODE"))
        .thenThrow(new TournamentNotFoundException());

    mockMvc
        .perform(get("/api/v1/tournaments/join-code/BADCODE").with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void listMyTournaments_returnsGroupedActiveTournamentSummaries() throws Exception {
    User principal = authenticatedUser();
    Tournament hosting = registrationTournament(UUID.randomUUID(), principal);
    Tournament playing =
        Tournament.builder()
            .id(UUID.randomUUID())
            .organizer(authenticatedUser())
            .title("Playing Cup")
            .joinCode("PLAY01")
        .participationMode(TournamentParticipationMode.SELF_JOIN)
            .status(TournamentStatus.IN_PROGRESS)
            .gameType(GameType.DOUBLES)
            .format(TournamentFormat.DOUBLE_ELIMINATION)
            .build();
    when(tournamentService.listActiveTournamentsForUser(any()))
        .thenReturn(new ActiveTournaments(java.util.List.of(hosting), java.util.List.of(playing)));

    mockMvc
        .perform(get("/api/v1/tournaments/me").with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hosting[0].id").value(hosting.getId().toString()))
        .andExpect(jsonPath("$.hosting[0].title").value("TestTournament"))
        .andExpect(jsonPath("$.hosting[0].status").value("REGISTRATION"))
        .andExpect(jsonPath("$.hosting[0].format").value("SINGLE_ELIMINATION"))
        .andExpect(jsonPath("$.hosting[0].gameType").value("SINGLES"))
        .andExpect(jsonPath("$.playing[0].id").value(playing.getId().toString()))
        .andExpect(jsonPath("$.playing[0].title").value("Playing Cup"))
        .andExpect(jsonPath("$.playing[0].status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.playing[0].format").value("DOUBLE_ELIMINATION"))
        .andExpect(jsonPath("$.playing[0].gameType").value("DOUBLES"));
  }

  @Test
  void listCompletedTournaments_returnsCompletedTournamentSummaries() throws Exception {
    User principal = authenticatedUser();
    Tournament completed =
        Tournament.builder()
            .id(UUID.randomUUID())
            .organizer(principal)
            .title("Old Tournament")
            .joinCode("OLD001")
        .participationMode(TournamentParticipationMode.SELF_JOIN)
            .status(TournamentStatus.COMPLETED)
            .gameType(GameType.SINGLES)
            .format(TournamentFormat.SINGLE_ELIMINATION)
            .build();
    when(tournamentService.listCompletedTournamentsForUser(any()))
        .thenReturn(java.util.List.of(completed));

    mockMvc
        .perform(get("/api/v1/tournaments/completed").with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(completed.getId().toString()))
        .andExpect(jsonPath("$[0].title").value("Old Tournament"))
        .andExpect(jsonPath("$[0].status").value("COMPLETED"))
        .andExpect(jsonPath("$[0].format").value("SINGLE_ELIMINATION"))
        .andExpect(jsonPath("$[0].gameType").value("SINGLES"))
        .andExpect(jsonPath("$[0].joinCode").doesNotExist())
        .andExpect(jsonPath("$[0].brackets").doesNotExist());
  }

  // ── Delete Tournament ─────────────────────────────────────────

  @Test
  void deleteTournament_whenFound_returnsNoContent() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doNothing().when(tournamentService).deleteTournament(eq(id), any());

    mockMvc
        .perform(delete("/api/v1/tournaments/{id}", id).with(user(principal)))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteTournament_whenNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentNotFoundException())
        .when(tournamentService)
        .deleteTournament(eq(id), any());

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
    doNothing().when(tournamentService).startTournament(eq(id), any());
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
        .startTournament(eq(id), any());

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
    doThrow(new TournamentNotFoundException())
        .when(tournamentService)
        .startTournament(eq(id), any());

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/start", id).with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void startTournament_whenUserIsNotOrganizer_returnsForbidden() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentAccessDeniedException(id))
        .when(tournamentService)
        .startTournament(eq(id), any());

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/start", id).with(user(principal)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("You are not allowed to modify tournament: " + id));
  }

  // ── Generate Bracket ──────────────────────────────────────────

  @Test
  void generateBracket_whenRegistration_returnsOkWithTournament() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doNothing().when(tournamentService).generateBracket(eq(id), any());
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
        .generateBracket(eq(id), any());

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
                "At least 3 participants are required."))
        .when(tournamentService)
        .generateBracket(eq(id), any());

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/bracket", id).with(user(principal)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("At least 3 participants are required."));
  }

  @Test
  void generateBracket_whenNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentNotFoundException())
        .when(tournamentService)
        .generateBracket(eq(id), any());

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
        .participationMode(TournamentParticipationMode.SELF_JOIN)
            .status(TournamentStatus.BRACKET_READY)
            .gameType(GameType.DOUBLES)
            .build();
    doNothing().when(tournamentService).generateBracket(eq(id), any());
    when(tournamentService.getTournament(id)).thenReturn(tournament);

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/bracket", id).with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value("BRACKET_READY"))
        .andExpect(jsonPath("$.gameType").value("DOUBLES"));
  }

  @Test
  void generateBracket_whenUserIsNotOrganizer_returnsForbidden() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentAccessDeniedException(id))
        .when(tournamentService)
        .generateBracket(eq(id), any());

    mockMvc
        .perform(post("/api/v1/tournaments/{id}/bracket", id).with(user(principal)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("You are not allowed to modify tournament: " + id));
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
    doNothing()
        .when(tournamentService)
        .removeParticipant(eq(tournamentId), any(), eq(participantId));

    mockMvc
        .perform(
            delete(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .with(user(principal)))
        .andExpect(status().isNoContent());
  }

  // ── Guest roster ──────────────────────────────────────────────

  @Test
  void addGuestParticipant_whenValid_returnsCreated() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    TournamentParticipant guest =
        TournamentParticipant.builder()
            .id(participantId)
            .displayName("Pat Riley")
            .build();
    when(tournamentService.addGuestParticipant(eq(tournamentId), any(), eq("Pat Riley")))
        .thenReturn(guest);
    when(tournamentMapper.toParticipantResponse(eq(guest), any()))
        .thenReturn(
            new TournamentParticipantResponse(participantId, null, "Pat Riley", false));

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/participants", tournamentId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("Pat Riley"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(participantId.toString()))
        .andExpect(jsonPath("$.displayName").value("Pat Riley"))
        .andExpect(jsonPath("$.username").doesNotExist());
  }

  @Test
  void addGuestParticipant_whenBlankDisplayName_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/participants", tournamentId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("  "))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addGuestParticipant_whenNotOrganizer_returnsForbidden() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    doThrow(new TournamentAccessDeniedException(tournamentId))
        .when(tournamentService)
        .addGuestParticipant(eq(tournamentId), any(), eq("Alex"));

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/participants", tournamentId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("Alex"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void addGuestParticipant_whenDuplicateName_returnsConflict() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    doThrow(new DuplicateGuestDisplayNameException("Alex"))
        .when(tournamentService)
        .addGuestParticipant(eq(tournamentId), any(), eq("Alex"));

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/participants", tournamentId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("Alex"))))
        .andExpect(status().isConflict());
  }

  @Test
  void updateGuestParticipant_whenValid_returnsOk() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    TournamentParticipant guest =
        TournamentParticipant.builder()
            .id(participantId)
            .displayName("Pat Riley")
            .build();
    when(tournamentService.updateGuestParticipant(
            eq(tournamentId), any(), eq(participantId), eq("Pat Riley")))
        .thenReturn(guest);
    when(tournamentMapper.toParticipantResponse(eq(guest), any()))
        .thenReturn(
            new TournamentParticipantResponse(participantId, null, "Pat Riley", false));

    mockMvc
        .perform(
            patch(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("Pat Riley"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Pat Riley"));
  }

  @Test
  void updateGuestParticipant_whenNotRegistration_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    doThrow(new InvalidTournamentStateException("Cannot update guests after registration"))
        .when(tournamentService)
        .updateGuestParticipant(eq(tournamentId), any(), eq(participantId), eq("Alex"));

    mockMvc
        .perform(
            patch(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("Alex"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Cannot update guests after registration"));
  }

  @Test
  void removeParticipant_whenNotRegistration_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    doThrow(new InvalidTournamentStateException("Cannot remove participants after registration"))
        .when(tournamentService)
        .removeParticipant(eq(tournamentId), any(), eq(participantId));

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
        .removeParticipant(eq(tournamentId), any(), eq(participantId));

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
        .removeParticipant(eq(tournamentId), any(), eq(participantId));

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

  @Test
  void removeParticipant_whenUserIsNotOrganizer_returnsForbidden() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    doThrow(new TournamentAccessDeniedException(tournamentId))
        .when(tournamentService)
        .removeParticipant(eq(tournamentId), any(), eq(participantId));

    mockMvc
        .perform(
            delete(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .with(user(principal)))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.message")
                .value("You are not allowed to modify tournament: " + tournamentId));
  }

  // ── Leave Tournament ──────────────────────────────────────────

  @Test
  void leaveTournament_whenValid_returnsNoContent() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    doNothing().when(tournamentService).leaveTournament(eq(tournamentId), any());

    mockMvc
        .perform(
            delete("/api/v1/tournaments/{id}/participants/me", tournamentId)
                .with(user(principal)))
        .andExpect(status().isNoContent());
  }

  @Test
  void leaveTournament_whenRegistrationClosed_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    doThrow(new InvalidTournamentStateException("Cannot leave tournament after registration"))
        .when(tournamentService)
        .leaveTournament(eq(tournamentId), any());

    mockMvc
        .perform(
            delete("/api/v1/tournaments/{id}/participants/me", tournamentId)
                .with(user(principal)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Cannot leave tournament after registration"));
  }

  @Test
  void leaveTournament_whenParticipantNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    doThrow(new TournamentParticipantNotFoundException())
        .when(tournamentService)
        .leaveTournament(eq(tournamentId), any());

    mockMvc
        .perform(
            delete("/api/v1/tournaments/{id}/participants/me", tournamentId)
                .with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament participant not found"));
  }

  @Test
  void leaveTournament_whenUserCannotAccess_returnsForbidden() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    doThrow(new TournamentAccessDeniedException(tournamentId))
        .when(tournamentService)
        .leaveTournament(eq(tournamentId), any());

    mockMvc
        .perform(
            delete("/api/v1/tournaments/{id}/participants/me", tournamentId)
                .with(user(principal)))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.message")
                .value("You are not allowed to modify tournament: " + tournamentId));
  }

  // ── Update Round Settings ─────────────────────────────────────

  @Test
  void updateRoundSettings_whenValid_returnsOkWithTournament() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doNothing().when(tournamentService).updateRoundSettings(eq(id), any(), eq(1), eq(3));
    when(tournamentService.getTournament(id)).thenReturn(bracketReadyTournament(id, principal));

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3))))
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
        .updateRoundSettings(eq(id), any(), eq(1), eq(3));

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3))))
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
        .updateRoundSettings(eq(id), any(), eq(1), eq(2));

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(2))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("bestOf must be one of: 1, 3, 5, or 7"));
  }

  @Test
  void updateRoundSettings_whenRoundNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentRoundNotFoundException())
        .when(tournamentService)
        .updateRoundSettings(eq(id), any(), eq(99), any());

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 99)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament round not found"));
  }

  @Test
  void updateRoundSettings_whenTournamentNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentNotFoundException())
        .when(tournamentService)
        .updateRoundSettings(eq(id), any(), eq(1), any());

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void updateRoundSettings_whenBestOfNotProvided_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new InvalidRoundConfigurationException("bestOf must be provided"))
        .when(tournamentService)
        .updateRoundSettings(eq(id), any(), eq(1), eq(null));

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("bestOf must be provided"));
  }

  @Test
  void updateRoundSettings_whenUserIsNotOrganizer_returnsForbidden() throws Exception {
    User principal = authenticatedUser();
    UUID id = UUID.randomUUID();
    doThrow(new TournamentAccessDeniedException(id))
        .when(tournamentService)
        .updateRoundSettings(eq(id), any(), eq(1), eq(3));

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", id, 1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("You are not allowed to modify tournament: " + id));
  }
}
