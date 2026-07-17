package com.github.solisa14.fourbagger.api.tournament;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestCookieHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * End-to-end coverage for organizer-managed guest roster management introduced in FB-11.
 */
class TournamentOrganizerManagedIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  void organizerManagedRoster_createAddListUpdateRemoveAndRejectUnauthorizedFlows()
      throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String organizerToken = registerAndGetToken("omorg" + suffix);
    String outsiderToken = registerAndGetToken("omout" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Managed Cup",
                                null,
                                TournamentFormat.SINGLE_ELIMINATION,
                                TournamentParticipationMode.ORGANIZER_MANAGED))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.participationMode").value("ORGANIZER_MANAGED"))
            .andExpect(jsonPath("$.joinCode").isEmpty())
            .andReturn();

    UUID tournamentId =
        UUID.fromString(
            objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

    MvcResult addResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments/{id}/participants", tournamentId)
                    .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(new GuestParticipantRequest("  Pat Riley  "))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.displayName").value("Pat Riley"))
            .andExpect(jsonPath("$.username").isEmpty())
            .andReturn();

    UUID guestId =
        UUID.fromString(
            objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText());

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/participants", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("Alex"))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participationMode").value("ORGANIZER_MANAGED"))
        .andExpect(jsonPath("$.participants.length()").value(2))
        .andExpect(jsonPath("$.participants[?(@.displayName == 'Alex')]").exists())
        .andExpect(jsonPath("$.participants[?(@.displayName == 'Pat Riley')]").exists());

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/participants", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("Casey"))))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/participants/{participantId}", tournamentId, guestId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new GuestParticipantRequest("  Patricia  "))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Patricia"));

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/participants", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("alex"))))
        .andExpect(status().isConflict());

    // Force a join code so the mode guard rejects account join (not merely "code missing").
    transactionTemplate.executeWithoutResult(
        status -> {
          Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow();
          tournament.setJoinCode("OMJOIN");
          tournamentRepository.save(tournament);
        });

    mockMvc
        .perform(
            post("/api/v1/tournaments/join")
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTournamentRequest("OMJOIN"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Organizer-managed tournaments do not allow account self-join"));

    mockMvc
        .perform(
            get("/api/v1/tournaments/join-code/{joinCode}", "OMJOIN")
                .cookie(TestCookieHelper.cookie("accessToken", outsiderToken)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Organizer-managed tournaments do not support join-code lookup"));

    mockMvc
        .perform(
            delete("/api/v1/tournaments/{id}/participants/{participantId}", tournamentId, guestId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/v1/tournaments/{id}", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participants.length()").value(1))
        .andExpect(jsonPath("$.participants[0].displayName").value("Alex"));

    transactionTemplate.executeWithoutResult(
        status -> {
          Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow();
          tournament.setStatus(TournamentStatus.BRACKET_READY);
          tournamentRepository.save(tournament);
        });

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/participants", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuestParticipantRequest("Casey"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Cannot add guests after registration"));
  }

  @Test
  void organizerManagedRoster_generateBracket_createsGuestBackedTeams() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String organizerToken = registerAndGetToken("ombracket" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Guest Bracket Cup",
                                null,
                                TournamentFormat.SINGLE_ELIMINATION,
                                TournamentParticipationMode.ORGANIZER_MANAGED))))
            .andExpect(status().isCreated())
            .andReturn();

    UUID tournamentId =
        UUID.fromString(
            objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

    for (String name : List.of("Pat", "Alex", "Casey", "Dana")) {
      mockMvc
          .perform(
              post("/api/v1/tournaments/{id}/participants", tournamentId)
                  .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new GuestParticipantRequest(name))))
          .andExpect(status().isCreated());
    }

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken)))
        .andExpect(status().isOk());

    MvcResult detail =
        mockMvc
            .perform(
                get("/api/v1/tournaments/{id}", tournamentId)
                    .cookie(TestCookieHelper.cookie("accessToken", organizerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BRACKET_READY"))
            .andExpect(jsonPath("$.brackets.winners[0].matches.length()").value(2))
            .andReturn();

    var matches =
        objectMapper
            .readTree(detail.getResponse().getContentAsString())
            .path("brackets")
            .path("winners")
            .get(0)
            .path("matches");
    Set<String> names = new HashSet<>();
    matches.forEach(
        match -> {
          if (match.hasNonNull("teamOne")) {
            names.add(match.path("teamOne").path("playerOneDisplayName").asText());
          }
          if (match.hasNonNull("teamTwo")) {
            names.add(match.path("teamTwo").path("playerOneDisplayName").asText());
          }
        });
    Assertions.assertThat(names).containsExactlyInAnyOrder("Pat", "Alex", "Casey", "Dana");
  }

  @Test
  void organizerManagedManualDoubles_setTeamsGenerateAndReshufflePreservesPairs() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String organizerToken = registerAndGetToken("ommanual" + suffix);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/tournaments")
                    .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateTournamentRequest(
                                "Manual Doubles Cup",
                                com.github.solisa14.fourbagger.api.game.GameType.DOUBLES,
                                TournamentFormat.SINGLE_ELIMINATION,
                                TournamentParticipationMode.ORGANIZER_MANAGED))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.doublesPairingMode").value("RANDOM"))
            .andReturn();

    UUID tournamentId =
        UUID.fromString(
            objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

    mockMvc
        .perform(
            patch("/api/v1/tournaments/{id}/doubles-pairing-mode", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UpdateDoublesPairingModeRequest(DoublesPairingMode.MANUAL))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doublesPairingMode").value("MANUAL"))
        .andExpect(jsonPath("$.participants").isEmpty())
        .andExpect(jsonPath("$.teams").isEmpty());

    mockMvc
        .perform(
            put("/api/v1/tournaments/{id}/manual-teams", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new ReplaceManualTeamsRequest(
                            List.of(
                                new ManualTeamRow("Pat", "Alex"),
                                new ManualTeamRow("Casey", "Dana"),
                                new ManualTeamRow("Riley", "Jordan"))))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participants.length()").value(6))
        .andExpect(jsonPath("$.teams.length()").value(3))
        .andExpect(jsonPath("$.bracketEligibility.eligible").value(true));

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BRACKET_READY"));

    Set<Set<String>> pairsBeforeReshuffle = readDoublesPairs(tournamentId, organizerToken);

    mockMvc
        .perform(
            post("/api/v1/tournaments/{id}/bracket", tournamentId)
                .cookie(TestCookieHelper.cookie("accessToken", organizerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BRACKET_READY"));

    Set<Set<String>> pairsAfterReshuffle = readDoublesPairs(tournamentId, organizerToken);
    Assertions.assertThat(pairsAfterReshuffle).isEqualTo(pairsBeforeReshuffle);
    Assertions.assertThat(pairsAfterReshuffle)
        .containsExactlyInAnyOrder(
            Set.of("Pat", "Alex"), Set.of("Casey", "Dana"), Set.of("Riley", "Jordan"));
  }

  private Set<Set<String>> readDoublesPairs(UUID tournamentId, String organizerToken)
      throws Exception {
    MvcResult detail =
        mockMvc
            .perform(
                get("/api/v1/tournaments/{id}", tournamentId)
                    .cookie(TestCookieHelper.cookie("accessToken", organizerToken)))
            .andExpect(status().isOk())
            .andReturn();

    var teams =
        objectMapper.readTree(detail.getResponse().getContentAsString()).path("teams");
    Set<Set<String>> pairs = new HashSet<>();
    teams.forEach(
        team ->
            pairs.add(
                Set.of(
                    team.path("playerOneDisplayName").asText(),
                    team.path("playerTwoDisplayName").asText())));
    return pairs;
  }
}
