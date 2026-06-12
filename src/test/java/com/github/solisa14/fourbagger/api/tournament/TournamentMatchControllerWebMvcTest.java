package com.github.solisa14.fourbagger.api.tournament;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.solisa14.fourbagger.api.common.exception.GlobalExceptionHandler;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TournamentMatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
  GlobalExceptionHandler.class,
  TournamentMapper.class,
  com.github.solisa14.fourbagger.api.game.GameMapper.class
})
class TournamentMatchControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @MockitoBean private TournamentMatchService tournamentMatchService;
  @MockitoBean private TournamentMatchResultService tournamentMatchResultService;
  @MockitoBean private com.github.solisa14.fourbagger.api.security.JwtService jwtService;
  @MockitoBean private com.github.solisa14.fourbagger.api.user.UserService userService;

  private User authenticatedUser() {
    return TestDataFactory.user(UUID.randomUUID(), "testuser", "encoded", Role.USER);
  }

  @Test
  void startMatch_whenValid_returnsOkWithMatchDetail() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();
    TournamentMatchDetailResponse detail = matchDetail(matchId);

    when(tournamentMatchService.startMatch(eq(tournamentId), eq(matchId), any()))
        .thenReturn(detail);

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(matchId.toString()))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.bestOf").value(1))
        .andExpect(jsonPath("$.nextGameNumber").value(1))
        .andExpect(jsonPath("$.results").isArray())
        .andExpect(jsonPath("$.lockState").doesNotExist());
  }

  @Test
  void getMatch_whenFound_returnsOkWithMatchDetail() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();
    UUID teamOneId = UUID.randomUUID();
    UUID teamTwoId = UUID.randomUUID();
    TournamentMatchDetailResponse detail =
        new TournamentMatchDetailResponse(
            matchId,
            1,
            MatchStatus.PENDING,
            false,
            teamSummary(teamOneId, "team1-a"),
            teamSummary(teamTwoId, "team2-a"),
            0,
            0,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            1,
            1,
            1,
            List.of());

    when(tournamentMatchService.getMatchDetail(tournamentId, matchId)).thenReturn(detail);

    mockMvc
        .perform(
            get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                .with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(matchId.toString()))
        .andExpect(jsonPath("$.results").isEmpty())
        .andExpect(jsonPath("$.lockState").doesNotExist());
  }

  @Test
  void submitResult_whenValid_returnsCreatedWithMatchDetail() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();
    UUID winnerTeamId = UUID.randomUUID();
    TournamentMatchDetailResponse detail = matchDetail(matchId);

    when(tournamentMatchResultService.submitResult(
            eq(tournamentId), eq(matchId), eq(1), any(), any()))
        .thenReturn(detail);

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new SubmitTournamentGameResultRequest(winnerTeamId, 21, 15))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(matchId.toString()));
  }

  @Test
  void submitResult_whenTeamOneScoreMissing_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();
    UUID winnerTeamId = UUID.randomUUID();
    String body =
        "{\"winnerTeamId\":\""
            + winnerTeamId
            + "\",\"teamTwoScore\":15}";

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitResult_whenTeamTwoScoreMissing_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();
    UUID winnerTeamId = UUID.randomUUID();
    String body =
        "{\"winnerTeamId\":\""
            + winnerTeamId
            + "\",\"teamOneScore\":21}";

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitResult_whenResultAlreadySubmitted_returnsConflict() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    when(tournamentMatchResultService.submitResult(
            eq(tournamentId), eq(matchId), eq(1), any(), any()))
        .thenThrow(new ResultAlreadySubmittedException(1));

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new SubmitTournamentGameResultRequest(UUID.randomUUID(), 21, 15))))
        .andExpect(status().isConflict());
  }

  @Test
  void correctResultEndpoint_isNotMapped() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    mockMvc
        .perform(
            patch(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/games/{gameNumber}/result",
                    tournamentId,
                    matchId,
                    1)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  void auditEndpoint_isNotMapped() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    mockMvc
        .perform(
            get(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/results/audit",
                    tournamentId,
                    matchId)
                .with(user(principal)))
        .andExpect(status().isNotFound());
  }

  private TournamentMatchDetailResponse matchDetail(UUID matchId) {
    return new TournamentMatchDetailResponse(
        matchId,
        1,
        MatchStatus.PENDING,
        false,
        null,
        null,
        0,
        0,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        1,
        1,
        1,
        List.of());
  }

  private MatchResponse.TeamSummary teamSummary(UUID id, String username) {
    return new MatchResponse.TeamSummary(id, username, null, 1, 0, false);
  }
}
