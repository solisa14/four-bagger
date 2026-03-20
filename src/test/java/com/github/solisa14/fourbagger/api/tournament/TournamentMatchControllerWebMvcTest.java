package com.github.solisa14.fourbagger.api.tournament;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.solisa14.fourbagger.api.common.exception.GlobalExceptionHandler;
import com.github.solisa14.fourbagger.api.game.Game;
import com.github.solisa14.fourbagger.api.game.GameStatus;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * WebMvc tests for {@link TournamentMatchController}. Validates HTTP status codes, response body
 * structure, and exception-to-status mapping for all match endpoints.
 */
@WebMvcTest(TournamentMatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
  GlobalExceptionHandler.class,
  TournamentMapper.class,
  com.github.solisa14.fourbagger.api.game.GameMapper.class
})
class TournamentMatchControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private TournamentMatchService tournamentMatchService;
  @MockitoBean private com.github.solisa14.fourbagger.api.security.JwtService jwtService;
  @MockitoBean private com.github.solisa14.fourbagger.api.user.UserService userService;

  private User authenticatedUser() {
    return TestDataFactory.user(
        UUID.randomUUID(), "testuser", "test@example.com", "encoded", Role.USER);
  }

  // ── Start Match ─────────────────────────────────────────────────

  @Test
  void startMatch_whenValid_returnsOkWithGameResponse() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();
    Game game =
        Game.builder()
            .id(UUID.randomUUID())
            .gameType(GameType.SINGLES)
            .playerOne(principal)
            .playerTwo(
                TestDataFactory.user(
                    UUID.randomUUID(), "opponent", "opp@example.com", "encoded", Role.USER))
            .status(GameStatus.PENDING)
            .targetScore(21)
            .build();

    when(tournamentMatchService.startMatch(tournamentId, matchId, principal)).thenReturn(game);

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(game.getId().toString()))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void startMatch_whenTournamentNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    when(tournamentMatchService.startMatch(tournamentId, matchId, principal))
        .thenThrow(new TournamentNotFoundException());

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void startMatch_whenMatchNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    when(tournamentMatchService.startMatch(tournamentId, matchId, principal))
        .thenThrow(new MatchNotFoundException(matchId));

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Match not found: " + matchId));
  }

  @Test
  void startMatch_whenInvalidTournamentState_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    when(tournamentMatchService.startMatch(tournamentId, matchId, principal))
        .thenThrow(
            new InvalidTournamentStateException(
                "Cannot start a match unless the tournament is IN_PROGRESS"));

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .with(user(principal)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Cannot start a match unless the tournament is IN_PROGRESS"));
  }

  @Test
  void startMatch_whenMatchIsBye_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    when(tournamentMatchService.startMatch(tournamentId, matchId, principal))
        .thenThrow(new InvalidTournamentStateException("Cannot start a bye match"));

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .with(user(principal)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Cannot start a bye match"));
  }

  @Test
  void startMatch_whenUserIsNotOrganizer_returnsForbidden() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    when(tournamentMatchService.startMatch(tournamentId, matchId, principal))
        .thenThrow(new TournamentAccessDeniedException(tournamentId));

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .with(user(principal)))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.message")
                .value("You are not allowed to modify tournament: " + tournamentId));
  }

  // ── Get Match ───────────────────────────────────────────────────

  @Test
  void getMatch_whenFound_returnsOkWithMatchResponse() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    Tournament tournament = tournament(tournamentId);
    Match match = match(matchId, tournament);

    when(tournamentMatchService.getMatch(tournamentId, matchId)).thenReturn(match);

    mockMvc
        .perform(
            get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                .with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(matchId.toString()))
        .andExpect(jsonPath("$.matchNumber").value(1))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.isBye").value(false))
        .andExpect(jsonPath("$.teamOne.playerOneUsername").value("team1-a"))
        .andExpect(jsonPath("$.teamTwo.playerOneUsername").value("team2-a"))
        .andExpect(jsonPath("$.teamOneWins").value(0))
        .andExpect(jsonPath("$.teamTwoWins").value(0))
        .andExpect(jsonPath("$.winner").isEmpty());
  }

  @Test
  void getMatch_whenMatchNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    when(tournamentMatchService.getMatch(tournamentId, matchId))
        .thenThrow(new MatchNotFoundException(matchId));

    mockMvc
        .perform(
            get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                .with(user(principal)))
        .andExpect(status().isNotFound());
  }

  @Test
  void getMatch_whenTournamentNotFound_returnsNotFound() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    when(tournamentMatchService.getMatch(tournamentId, matchId))
        .thenThrow(new TournamentNotFoundException());

    mockMvc
        .perform(
            get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                .with(user(principal)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void getMatch_whenMatchBelongsToDifferentTournament_returnsBadRequest() throws Exception {
    User principal = authenticatedUser();
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    when(tournamentMatchService.getMatch(tournamentId, matchId))
        .thenThrow(new InvalidTournamentStateException("Match does not belong to this tournament"));

    mockMvc
        .perform(
            get("/api/v1/tournaments/{tournamentId}/matches/{matchId}", tournamentId, matchId)
                .with(user(principal)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Match does not belong to this tournament"));
  }

  // ── Test helpers ────────────────────────────────────────────────

  private Tournament tournament(UUID id) {
    User organizer =
        TestDataFactory.user(
            UUID.randomUUID(), "organizer", "org@example.com", "encoded", Role.USER);
    return Tournament.builder()
        .id(id)
        .organizer(organizer)
        .title("Tournament")
        .status(TournamentStatus.IN_PROGRESS)
        .joinCode("ABC123")
        .build();
  }

  private Match match(UUID matchId, Tournament tournament) {
    TournamentRound round =
        TournamentRound.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(1)
            .bestOf(1)
            .scoringMode(ScoringMode.STANDARD)
            .build();
    TournamentTeam teamOne =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(testUser("team1-a"))
            .seed(1)
            .build();
    TournamentTeam teamTwo =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(testUser("team2-a"))
            .seed(2)
            .build();

    return Match.builder()
        .id(matchId)
        .round(round)
        .teamOne(teamOne)
        .teamTwo(teamTwo)
        .matchNumber(1)
        .status(MatchStatus.PENDING)
        .build();
  }

  private User testUser(String suffix) {
    return TestDataFactory.user(
        UUID.randomUUID(), suffix, suffix + "@example.com", "encoded", Role.USER);
  }
}
