package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SharedTournamentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest
    @CsvSource({
        "SELF_JOIN,SINGLE_ELIMINATION,IN_PROGRESS",
        "SELF_JOIN,DOUBLE_ELIMINATION,IN_PROGRESS",
        "ORGANIZER_MANAGED,SINGLE_ELIMINATION,IN_PROGRESS",
        "ORGANIZER_MANAGED,DOUBLE_ELIMINATION,IN_PROGRESS",
        "SELF_JOIN,SINGLE_ELIMINATION,COMPLETED"
    })
    void sharedTournament_whenAvailable_returnsSafeAnonymousBracket(
            TournamentParticipationMode participationMode,
            TournamentFormat format,
            TournamentStatus tournamentStatus) throws Exception {
        UUID shareId = UUID.randomUUID();
        Tournament tournament = tournament(participationMode, format, tournamentStatus, shareId);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String labelOne = participationMode == TournamentParticipationMode.SELF_JOIN
                ? "account-one-" + suffix
                : "Guest One";
        String labelTwo = participationMode == TournamentParticipationMode.SELF_JOIN
                ? "account-two-" + suffix
                : "Guest Two";
        TournamentParticipant playerOne = participant(tournament, participationMode, labelOne);
        TournamentParticipant playerTwo = participant(tournament, participationMode, labelTwo);
        tournament.getParticipants().addAll(List.of(playerOne, playerTwo));
        TournamentTeam team = TournamentTeam.builder()
                .tournament(tournament)
                .playerOne(playerOne)
                .playerTwo(playerTwo)
                .seed(1)
                .build();
        tournament.getTeams().add(team);
        TournamentRound round = TournamentRound.builder()
                .tournament(tournament)
                .bracketType(BracketType.WINNERS)
                .roundNumber(1)
                .bestOf(3)
                .build();
        round.getMatches().add(Match.builder()
                .round(round)
                .matchNumber(1)
                .teamOne(team)
                .teamOneWins(2)
                .status(MatchStatus.COMPLETED)
                .winner(team)
                .build());
        tournament.getRounds().add(round);
        tournamentRepository.saveAndFlush(tournament);

        mockMvc.perform(get("/api/v1/shared-tournaments/{shareId}", shareId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.format").value(format.name()))
                .andExpect(jsonPath("$.brackets.winners[0].matches[0].teamOne.participantLabels[0]")
                        .value(labelOne))
                .andExpect(jsonPath("$.brackets.winners[0].matches[0].teamOne.participantLabels[1]")
                        .value(labelTwo))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.joinCode").doesNotExist())
                .andExpect(jsonPath("$.participants").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(value = TournamentStatus.class, names = {"REGISTRATION", "BRACKET_READY"})
    void sharedTournament_whenStatusUnavailable_returnsNoStoreNotFound(TournamentStatus status) throws Exception {
        UUID shareId = UUID.randomUUID();
        tournamentRepository.saveAndFlush(
                tournament(TournamentParticipationMode.ORGANIZER_MANAGED, TournamentFormat.SINGLE_ELIMINATION, status, shareId));

        mockMvc.perform(get("/api/v1/shared-tournaments/{shareId}", shareId))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.message").value("Tournament not found"));
    }

    @Test
    void sharedTournamentRoute_whenUnauthenticated_rejectsNonGetRequests() throws Exception {
        mockMvc.perform(post("/api/v1/shared-tournaments/{shareId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private Tournament tournament(
            TournamentParticipationMode participationMode,
            TournamentFormat format,
            TournamentStatus status,
            UUID shareId) {
        User organizer = userRepository.save(TestDataFactory.user(
                null, "organizer-" + UUID.randomUUID(), "encoded", Role.USER));
        return Tournament.builder()
                .organizer(organizer)
                .title("Shared Tournament")
                .joinCode(participationMode == TournamentParticipationMode.SELF_JOIN
                        ? UUID.randomUUID().toString().substring(0, 6)
                        : null)
                .participationMode(participationMode)
                .status(status)
                .gameType(GameType.DOUBLES)
                .format(format)
                .shareId(shareId)
                .build();
    }

    private TournamentParticipant participant(
            Tournament tournament, TournamentParticipationMode participationMode, String label) {
        if (participationMode == TournamentParticipationMode.ORGANIZER_MANAGED) {
            return TournamentParticipant.builder()
                    .tournament(tournament)
                    .displayName(label)
                    .build();
        }
        User user = userRepository.save(TestDataFactory.user(null, label, "encoded", Role.USER));
        return TournamentParticipant.builder()
                .tournament(tournament)
                .user(user)
                .build();
    }
}
