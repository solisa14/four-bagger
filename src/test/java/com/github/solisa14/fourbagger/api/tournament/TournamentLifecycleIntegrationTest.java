package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestCookieHelper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration tests verifying the full tournament lifecycle through HTTP endpoints. Covers create,
 * join, bracket generation, round configuration, start, and delete, as well as targeted error-case
 * scenarios.
 */
class TournamentLifecycleIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private TournamentTeamRepository tournamentTeamRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  void fullTournamentLifecycle_createJoinBracketConfigStartAndVerify() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);

    // 1. Register organizer + 3 players
    String orgToken = registerAndGetToken("lcorg" + suffix);
    String p1Token = registerAndGetToken("lcp1" + suffix);
    String p2Token = registerAndGetToken("lcp2" + suffix);
    String p3Token = registerAndGetToken("lcp3" + suffix);

    // 2. Create tournament
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Lifecycle Test", null, TournamentFormat.SINGLE_ELIMINATION, TournamentParticipationMode.SELF_JOIN))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("REGISTRATION"))
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String tournamentId = tournamentJson.get("id").asText();
    String joinCode = tournamentJson.get("joinCode").asText();

    // 3. Join tournament with 3 players
    for (String playerToken : new String[] {p1Token, p2Token, p3Token}) {
      mockMvc
          .perform(
              post("/api/v1/tournaments/join")
                  .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(tournamentId));
    }

    // 4. Generate bracket
    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BRACKET_READY"))
        .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
        .andExpect(jsonPath("$.brackets.winners").isNotEmpty())
        .andExpect(jsonPath("$.rounds").doesNotExist());

    // 5. Update round 1 settings to best-of-3
    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", tournamentId, 1)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BRACKET_READY"));

    // 6. Start tournament
    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/start", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    // 7. Verify bracket structure via GET
    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
        .andExpect(jsonPath("$.brackets.winners").isArray())
        .andExpect(jsonPath("$.brackets.winners.length()").value(2))
        .andExpect(jsonPath("$.brackets.winners[0].bracketType").value("WINNERS"))
        .andExpect(jsonPath("$.brackets.winners[0].bestOf").value(3))
        .andExpect(jsonPath("$.brackets.winners[0].matches").isNotEmpty())
        .andExpect(jsonPath("$.rounds").doesNotExist());

    // 8. Delete tournament
    mockMvc
        .perform(
            delete("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isNoContent());

    // 9. Verify deletion
    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isNotFound());
  }

  @Test
  void organizerOnlyEndpoints_whenCalledByOutsider_returnForbidden() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("guardorg" + suffix);
    String outsiderToken = registerAndGetToken("guardout" + suffix);
    String p1Token = registerAndGetToken("guardp1" + suffix);
    String p2Token = registerAndGetToken("guardp2" + suffix);
    String p3Token = registerAndGetToken("guardp3" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Guard Test", null, TournamentFormat.SINGLE_ELIMINATION, TournamentParticipationMode.SELF_JOIN))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();

    for (String playerToken : new String[] {p1Token, p2Token, p3Token}) {
      mockMvc
          .perform(
              post("/api/v1/tournaments/join")
                  .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
          .andExpect(status().isOk());
    }

    UUID participantId =
        transactionTemplate.execute(
            status ->
                tournamentRepository
                    .findById(tournamentId)
                    .orElseThrow()
                    .getParticipants()
                    .getFirst()
                    .getId());

    mockMvc
        .perform(
            delete(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", tournamentId, 1)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3))))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/start", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/start", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk());

    UUID matchId =
        matchRepository
            .findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(tournamentId)
            .stream()
            .filter(match -> !match.isBye())
            .findFirst()
            .orElseThrow()
            .getId();

    mockMvc
        .perform(
            post(
                    "/api/v1/tournaments/{tournamentId}/matches/{matchId}/start",
                    tournamentId,
                    matchId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken)))
        .andExpect(status().isForbidden());
  }

  @Test
  void joinTournament_whenInvalidCode_returnsNotFound() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String token = registerAndGetToken("badcode" + suffix);

    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .cookie(TestCookieHelper.cookie("accessToken", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest("ZZZZZ9"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void joinTournament_whenAlreadyJoined_returnsConflict() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("duporg" + suffix);
    String playerToken = registerAndGetToken("duppl" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Dup Test", null, TournamentFormat.SINGLE_ELIMINATION, TournamentParticipationMode.SELF_JOIN))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andReturn();

    String joinCode =
        objectMapper
            .readTree(createResult.getResponse().getContentAsString())
            .get("joinCode")
            .asText();

    // First join succeeds
    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
        .andExpect(status().isOk());

    // Second join returns conflict
    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("User is already registered in this tournament"));
  }

  @Test
  void removeParticipant_removesAndAllowsRejoin() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("rmorg" + suffix);
    String playerToken = registerAndGetToken("rmpl" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Remove Test", null, TournamentFormat.SINGLE_ELIMINATION, TournamentParticipationMode.SELF_JOIN))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andReturn();

    var json = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String tournamentId = json.get("id").asText();
    String joinCode = json.get("joinCode").asText();

    // Join tournament
    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
        .andExpect(status().isOk());

    MvcResult detailResult =
        mockMvc
            .perform(
                get("/api/v1/tournaments/{id}", tournamentId)
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participants.length()").value(1))
            .andExpect(jsonPath("$.bracketEligibility.participantCount").value(1))
            .andReturn();

    String participantId =
        objectMapper
            .readTree(detailResult.getResponse().getContentAsString())
            .get("participants")
            .get(0)
            .get("id")
            .asText();

    mockMvc
        .perform(
            delete(
                    "/api/v1/tournaments/{id}/participants/{participantId}",
                    tournamentId,
                    participantId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participants").isEmpty())
        .andExpect(jsonPath("$.bracketEligibility.participantCount").value(0));

    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
        .andExpect(status().isOk());
  }

  @Test
  void leaveTournament_removesParticipantAccessForNonOrganizer() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("lvorg" + suffix);
    String playerToken = registerAndGetToken("lvpl" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Leave Test", null, TournamentFormat.SINGLE_ELIMINATION, TournamentParticipationMode.SELF_JOIN))))
            .andExpect(status().isCreated())
            .andReturn();

    var json = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String tournamentId = json.get("id").asText();
    String joinCode = json.get("joinCode").asText();

    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            delete("/api/v1/tournaments/{id}/participants/me", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", playerToken)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", playerToken)))
        .andExpect(status().isForbidden());
  }

  @Test
  void generateBracket_whenIneligible_returnsBadRequestWithoutPersistedTeams() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("inelorg" + suffix);
    String p1Token = registerAndGetToken("inelp1" + suffix);
    String p2Token = registerAndGetToken("inelp2" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Ineligible Test", null, TournamentFormat.SINGLE_ELIMINATION, TournamentParticipationMode.SELF_JOIN))))
            .andExpect(status().isCreated())
            .andReturn();

    var json = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(json.get("id").asText());
    String joinCode = json.get("joinCode").asText();

    for (String playerToken : new String[] {p1Token, p2Token}) {
      mockMvc
          .perform(
              post("/api/v1/tournaments/join")
                  .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
          .andExpect(status().isOk());
    }

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("At least 3 participants are required."));

    transactionTemplate.executeWithoutResult(
        status -> {
          Tournament tournament =
              tournamentRepository.findById(tournamentId).orElseThrow();
          assertThat(tournament.getTeams()).isEmpty();
          assertThat(tournament.getRounds()).isEmpty();
          assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.REGISTRATION);
        });
  }

  @Test
  void reshuffleBracket_whenSingleElimination_replacesPersistedAssignmentsAndPreservesSettings()
      throws Exception {
    TournamentSetup setup =
        createTournamentWithPlayers(
            "Single Reshuffle Test", GameType.SINGLES, TournamentFormat.SINGLE_ELIMINATION, 4);

    generateBracket(setup);
    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/rounds/{roundNumber}", setup.tournamentId(), 1)
                .cookie(TestCookieHelper.cookie("accessToken", setup.organizerToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoundSettingsRequest(3))))
        .andExpect(status().isOk());
    PersistedBracketSnapshot before = bracketSnapshot(setup.tournamentId());

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", setup.tournamentId())
                .cookie(TestCookieHelper.cookie("accessToken", setup.organizerToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BRACKET_READY"))
        .andExpect(jsonPath("$.brackets.winners[0].bestOf").value(3))
        .andExpect(jsonPath("$.brackets.winners[0].matches").isNotEmpty());

    PersistedBracketSnapshot after = bracketSnapshot(setup.tournamentId());
    assertRegenerated(before, after);
    assertThat(after.seeds()).containsExactlyInAnyOrder(1, 2, 3, 4);
    assertThat(after.bracketTypes()).containsOnly(BracketType.WINNERS);
  }

  @Test
  void reshuffleBracket_whenDoubleElimination_replacesPersistedAssignmentsAndRoutes()
      throws Exception {
    TournamentSetup setup =
        createTournamentWithPlayers(
            "Double Reshuffle Test", GameType.SINGLES, TournamentFormat.DOUBLE_ELIMINATION, 4);

    generateBracket(setup);
    PersistedBracketSnapshot before = bracketSnapshot(setup.tournamentId());

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", setup.tournamentId())
                .cookie(TestCookieHelper.cookie("accessToken", setup.organizerToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BRACKET_READY"))
        .andExpect(jsonPath("$.brackets.winners").isNotEmpty())
        .andExpect(jsonPath("$.brackets.losers").isNotEmpty())
        .andExpect(jsonPath("$.brackets.finalRounds").isNotEmpty());

    PersistedBracketSnapshot after = bracketSnapshot(setup.tournamentId());
    assertRegenerated(before, after);
    assertThat(after.seeds()).containsExactlyInAnyOrder(1, 2, 3, 4);
    assertThat(after.bracketTypes())
        .containsExactlyInAnyOrder(
            BracketType.WINNERS,
            BracketType.WINNERS,
            BracketType.LOSERS,
            BracketType.LOSERS,
            BracketType.FINAL,
            BracketType.GRAND_FINAL);
  }

  @Test
  void deleteTournament_whenNotFound_returnsNotFound() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String token = registerAndGetToken("delnf" + suffix);

    mockMvc
        .perform(
            delete("/api/v1/tournaments/{id}", UUID.randomUUID())
                .cookie(TestCookieHelper.cookie("accessToken", token)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Tournament not found"));
  }

  @Test
  void doublesTournamentLifecycle_createJoinBracketStartAndVerify() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);

    // 1. Register organizer + 6 players
    String orgToken = registerAndGetToken("dlcorg" + suffix);
    String[] playerTokens = new String[6];
    for (int i = 0; i < 6; i++) {
      playerTokens[i] = registerAndGetToken("dlcp" + i + suffix);
    }

    // 2. Create DOUBLES tournament
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Doubles Test",
                                GameType.DOUBLES,
                                TournamentFormat.SINGLE_ELIMINATION, TournamentParticipationMode.SELF_JOIN))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.gameType").value("DOUBLES"))
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andExpect(jsonPath("$.status").value("REGISTRATION"))
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String tournamentId = tournamentJson.get("id").asText();
    String joinCode = tournamentJson.get("joinCode").asText();

    // 3. Join 6 players
    for (String playerToken : playerTokens) {
      mockMvc
          .perform(
              post("/api/v1/tournaments/join")
                  .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(tournamentId));
    }

    // 4. Generate bracket — 6 players pair into 3 teams
    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BRACKET_READY"))
        .andExpect(jsonPath("$.gameType").value("DOUBLES"))
        .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
        .andExpect(jsonPath("$.brackets.winners").isNotEmpty())
        .andExpect(jsonPath("$.rounds").doesNotExist());

    // 5. Start tournament
    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/start", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    // 6. Verify bracket structure — 3 teams produce 2 rounds
    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.gameType").value("DOUBLES"))
        .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
        .andExpect(jsonPath("$.brackets.winners.length()").value(2))
        .andExpect(jsonPath("$.brackets.winners[0].bracketType").value("WINNERS"))
        .andExpect(jsonPath("$.brackets.winners[0].matches").isNotEmpty())
        .andExpect(jsonPath("$.rounds").doesNotExist());
  }

  @Test
  void startTournament_whenNotBracketReady_returnsBadRequest() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String orgToken = registerAndGetToken("stnbr" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", orgToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Start Early Test", null, TournamentFormat.SINGLE_ELIMINATION, TournamentParticipationMode.SELF_JOIN))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
            .andReturn();

    String tournamentId =
        objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

    // Try to start without generating bracket first
    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/start", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", orgToken)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("Tournament can only be started when bracket is ready"));
  }

  private TournamentSetup createTournamentWithPlayers(
      String title, GameType gameType, TournamentFormat format, int participantCount)
      throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String organizerToken = registerAndGetToken("rshorg" + suffix);
    String[] playerTokens = new String[participantCount];
    for (int i = 0; i < participantCount; i++) {
      playerTokens[i] = registerAndGetToken("rshp" + i + suffix);
    }

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(title, gameType, format, TournamentParticipationMode.SELF_JOIN))))
            .andExpect(status().isCreated())
            .andReturn();

    var tournamentJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID tournamentId = UUID.fromString(tournamentJson.get("id").asText());
    String joinCode = tournamentJson.get("joinCode").asText();
    for (String playerToken : playerTokens) {
      mockMvc
          .perform(
              post("/api/v1/tournaments/join")
                  .cookie(TestCookieHelper.cookie("accessToken", playerToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new JoinTournamentRequest(joinCode))))
          .andExpect(status().isOk());
    }

    return new TournamentSetup(organizerToken, tournamentId);
  }

  private void generateBracket(TournamentSetup setup) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", setup.tournamentId())
                .cookie(TestCookieHelper.cookie("accessToken", setup.organizerToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BRACKET_READY"));
  }

  private PersistedBracketSnapshot bracketSnapshot(UUID tournamentId) {
    return transactionTemplate.execute(
        status -> {
          Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow();
          List<UUID> teamIds =
              tournament.getTeams().stream().map(TournamentTeam::getId).toList();
          List<Integer> seeds =
              tournament.getTeams().stream().map(TournamentTeam::getSeed).toList();
          List<BracketType> bracketTypes =
              tournament.getRounds().stream().map(TournamentRound::getBracketType).toList();
          List<UUID> matchIds =
              matchRepository
                  .findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(tournamentId)
                  .stream()
                  .map(Match::getId)
                  .toList();
          return new PersistedBracketSnapshot(teamIds, matchIds, seeds, bracketTypes);
        });
  }

  private void assertRegenerated(
      PersistedBracketSnapshot before, PersistedBracketSnapshot after) {
    assertThat(after.teamIds()).hasSameSizeAs(before.teamIds());
    assertThat(after.matchIds()).hasSameSizeAs(before.matchIds());
    assertThat(after.teamIds()).doesNotContainAnyElementsOf(before.teamIds());
    assertThat(after.matchIds()).doesNotContainAnyElementsOf(before.matchIds());
    assertThat(tournamentTeamRepository.findAllById(before.teamIds())).isEmpty();
    assertThat(matchRepository.findAllById(before.matchIds())).isEmpty();
  }

  private record TournamentSetup(String organizerToken, UUID tournamentId) {}

  private record PersistedBracketSnapshot(
      List<UUID> teamIds,
      List<UUID> matchIds,
      List<Integer> seeds,
      List<BracketType> bracketTypes) {}
}
