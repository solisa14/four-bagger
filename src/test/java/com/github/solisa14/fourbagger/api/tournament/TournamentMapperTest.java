package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.solisa14.fourbagger.api.testsupport.TestDataFactory.user;
import static org.assertj.core.api.Assertions.assertThat;

class TournamentMapperTest {

    private final TournamentMapper mapper = new TournamentMapper(new TournamentBracketEligibilityPolicy());

    @Test
    void toTeamSummary_whenGuestMembers_exposesDisplayNames() {
        Tournament tournament = Tournament.builder()
                .id(UUID.randomUUID())
                .title("Tournament")
                .participationMode(TournamentParticipationMode.ORGANIZER_MANAGED)
                .status(TournamentStatus.IN_PROGRESS)
                .format(TournamentFormat.SINGLE_ELIMINATION)
                .build();
        TournamentTeam team = TournamentTeam.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .playerOne(TournamentParticipant.builder()
                        .id(UUID.randomUUID())
                        .tournament(tournament)
                        .displayName("Pat Riley")
                        .build())
                .playerTwo(TournamentParticipant.builder()
                        .id(UUID.randomUUID())
                        .tournament(tournament)
                        .displayName("Alex")
                        .build())
                .seed(1)
                .build();

        MatchResponse.TeamSummary summary = mapper.toTeamSummary(team);

        assertThat(summary.playerOneUsername()).isNull();
        assertThat(summary.playerOneDisplayName()).isEqualTo("Pat Riley");
        assertThat(summary.playerTwoUsername()).isNull();
        assertThat(summary.playerTwoDisplayName()).isEqualTo("Alex");
    }

    @Test
    void toTournamentResponse_whenResetIsInactive_hidesRoundAndIncomingRoutes() {
        Tournament tournament = tournament();
        TournamentRound finalRound = round(tournament, BracketType.FINAL);
        TournamentRound resetRound = round(tournament, BracketType.GRAND_FINAL);
        Match reset = match(resetRound, null, null);
        Match firstFinal = match(finalRound, team(tournament, "one"), team(tournament, "two"));
        firstFinal.setWinnerNextMatch(reset);
        firstFinal.setWinnerNextMatchPosition(2);
        firstFinal.setLoserNextMatch(reset);
        firstFinal.setLoserNextMatchPosition(1);
        finalRound.getMatches().add(firstFinal);
        resetRound.getMatches().add(reset);
        tournament.getRounds().addAll(List.of(finalRound, resetRound));

        TournamentResponse response = mapper.toTournamentResponse(tournament);

        assertThat(response.brackets().grandFinal()).isEmpty();
        MatchResponse firstFinalResponse =
                response.brackets().finalRounds().getFirst().matches().getFirst();
        assertThat(firstFinalResponse.winnerNextMatchId()).isNull();
        assertThat(firstFinalResponse.winnerNextMatchPosition()).isNull();
        assertThat(firstFinalResponse.loserNextMatchId()).isNull();
        assertThat(firstFinalResponse.loserNextMatchPosition()).isNull();
    }

    @Test
    void toMatchDetailResponse_mapsResultHistoryWithoutCorrectionFields() {
        Tournament tournament = tournament();
        TournamentTeam teamOne = team(tournament, "one");
        TournamentTeam teamTwo = team(tournament, "two");
        TournamentRound round = round(tournament, BracketType.WINNERS);
        Match match = match(round, teamOne, teamTwo);
        User submitter = teamOne.getPlayerOne().getUser();
        TournamentGameResult result = TournamentGameResult.builder()
                .id(UUID.randomUUID())
                .match(match)
                .gameNumber(1)
                .winnerTeam(teamOne)
                .teamOneScore(21)
                .teamTwoScore(15)
                .submittedBy(submitter)
                .submittedAt(Instant.parse("2026-01-01T12:00:00Z"))
                .build();

        TournamentMatchDetailResponse response = mapper.toMatchDetailResponse(match, List.of(result), 2);

        assertThat(response.results()).hasSize(1);
        TournamentGameResultResponse mapped = response.results().getFirst();
        assertThat(mapped.gameNumber()).isEqualTo(1);
        assertThat(mapped.winnerTeamId()).isEqualTo(teamOne.getId());
        assertThat(mapped.teamOneScore()).isEqualTo(21);
        assertThat(mapped.teamTwoScore()).isEqualTo(15);
        assertThat(mapped.submittedBy()).isEqualTo(submitter.getId());
        assertThat(mapped.submittedAt()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
        assertThat(response.nextGameNumber()).isEqualTo(2);
        assertThat(response.bestOf()).isEqualTo(1);
        assertThat(response.winsToClinch()).isEqualTo(1);
    }

    @Test
    void toTournamentResponse_whenResetIsActive_exposesRoundAndIncomingRoutes() {
        Tournament tournament = tournament();
        TournamentTeam teamOne = team(tournament, "one");
        TournamentTeam teamTwo = team(tournament, "two");
        TournamentRound finalRound = round(tournament, BracketType.FINAL);
        TournamentRound resetRound = round(tournament, BracketType.GRAND_FINAL);
        Match reset = match(resetRound, teamOne, teamTwo);
        Match firstFinal = match(finalRound, teamOne, teamTwo);
        firstFinal.setWinnerNextMatch(reset);
        firstFinal.setWinnerNextMatchPosition(2);
        firstFinal.setLoserNextMatch(reset);
        firstFinal.setLoserNextMatchPosition(1);
        finalRound.getMatches().add(firstFinal);
        resetRound.getMatches().add(reset);
        tournament.getRounds().addAll(List.of(finalRound, resetRound));

        TournamentResponse response = mapper.toTournamentResponse(tournament);

        assertThat(response.brackets().grandFinal()).hasSize(1);
        MatchResponse firstFinalResponse =
                response.brackets().finalRounds().getFirst().matches().getFirst();
        assertThat(firstFinalResponse.winnerNextMatchId()).isEqualTo(reset.getId());
        assertThat(firstFinalResponse.winnerNextMatchPosition()).isEqualTo(2);
        assertThat(firstFinalResponse.loserNextMatchId()).isEqualTo(reset.getId());
        assertThat(firstFinalResponse.loserNextMatchPosition()).isEqualTo(1);
    }

    @Test
    void toTournamentDetailResponse_mapsParticipantsSortedCaseInsensitively() {
        Tournament tournament = registrationTournament();
        User organizer = user(UUID.randomUUID(), "organizer", "encoded", Role.USER);
        tournament.setOrganizer(organizer);
        User alice = user(UUID.randomUUID(), "alice", "encoded", Role.USER);
        User bob = user(UUID.randomUUID(), "Bob", "encoded", Role.USER);
        tournament
                .getParticipants()
                .addAll(List.of(
                        participant(tournament, bob, UUID.randomUUID()),
                        participant(tournament, alice, UUID.randomUUID())));

        TournamentDetailResponse response = mapper.toTournamentDetailResponse(tournament, alice);

        assertThat(response.participants()).hasSize(2);
        assertThat(response.participants().get(0).username()).isEqualTo("alice");
        assertThat(response.participants().get(0).displayName()).isNull();
        assertThat(response.participants().get(0).currentViewer()).isTrue();
        assertThat(response.participants().get(1).username()).isEqualTo("Bob");
        assertThat(response.participants().get(1).displayName()).isNull();
        assertThat(response.participants().get(1).currentViewer()).isFalse();
    }

    @Test
    void toTournamentDetailResponse_mapsGuestParticipantsWithDisplayNameOnly() {
        Tournament tournament = Tournament.builder()
                .id(UUID.randomUUID())
                .title("Managed Cup")
                .participationMode(TournamentParticipationMode.ORGANIZER_MANAGED)
                .status(TournamentStatus.REGISTRATION)
                .format(TournamentFormat.SINGLE_ELIMINATION)
                .gameType(GameType.SINGLES)
                .build();
        User organizer = user(UUID.randomUUID(), "organizer", "encoded", Role.USER);
        tournament.setOrganizer(organizer);
        tournament.addGuestParticipant("Alex");
        tournament.addGuestParticipant("blake");

        TournamentDetailResponse response = mapper.toTournamentDetailResponse(tournament, organizer);

        assertThat(response.participationMode()).isEqualTo(TournamentParticipationMode.ORGANIZER_MANAGED);
        assertThat(response.joinCode()).isNull();
        assertThat(response.participants()).hasSize(2);
        assertThat(response.participants().get(0).username()).isNull();
        assertThat(response.participants().get(0).displayName()).isEqualTo("Alex");
        assertThat(response.participants().get(0).currentViewer()).isFalse();
        assertThat(response.participants().get(1).username()).isNull();
        assertThat(response.participants().get(1).displayName()).isEqualTo("blake");
    }

    @Test
    void toTournamentDetailResponse_organizerCapabilitiesDuringRegistration() {
        Tournament tournament = registrationTournament();
        User organizer = user(UUID.randomUUID(), "organizer", "encoded", Role.USER);
        tournament.setOrganizer(organizer);
        for (int i = 0; i < 3; i++) {
            tournament
                    .getParticipants()
                    .add(participant(
                            tournament,
                            user(UUID.randomUUID(), "player" + i, "encoded", Role.USER),
                            UUID.randomUUID()));
        }

        TournamentDetailResponse response = mapper.toTournamentDetailResponse(tournament, organizer);

        assertThat(response.viewerCapabilities().canManageTournament()).isTrue();
        assertThat(response.viewerCapabilities().canGenerateBracket()).isTrue();
        assertThat(response.viewerCapabilities().canRemoveParticipants()).isTrue();
        assertThat(response.viewerCapabilities().canLeaveRegistration()).isFalse();
        assertThat(response.viewerCapabilities().canOverrideMatchResults()).isFalse();
        assertThat(response.bracketEligibility().eligible()).isTrue();
    }

    @Test
    void toTournamentDetailResponse_participantCapabilitiesDuringRegistration() {
        Tournament tournament = registrationTournament();
        User organizer = user(UUID.randomUUID(), "organizer", "encoded", Role.USER);
        tournament.setOrganizer(organizer);
        User participantUser = user(UUID.randomUUID(), "player", "encoded", Role.USER);
        tournament.getParticipants().add(participant(tournament, participantUser, UUID.randomUUID()));

        TournamentDetailResponse response = mapper.toTournamentDetailResponse(tournament, participantUser);

        assertThat(response.viewerCapabilities().canManageTournament()).isFalse();
        assertThat(response.viewerCapabilities().canGenerateBracket()).isFalse();
        assertThat(response.viewerCapabilities().canRemoveParticipants()).isFalse();
        assertThat(response.viewerCapabilities().canLeaveRegistration()).isTrue();
    }

    @Test
    void toTournamentDetailResponse_organizerParticipantCanLeaveRegistration() {
        Tournament tournament = registrationTournament();
        User organizer = user(UUID.randomUUID(), "organizer", "encoded", Role.USER);
        tournament.setOrganizer(organizer);
        tournament.getParticipants().add(participant(tournament, organizer, UUID.randomUUID()));

        TournamentDetailResponse response = mapper.toTournamentDetailResponse(tournament, organizer);

        assertThat(response.viewerCapabilities().canLeaveRegistration()).isTrue();
        assertThat(response.viewerCapabilities().canGenerateBracket()).isFalse();
    }

    @Test
    void toTournamentDetailResponse_bracketReadyOrganizerCannotGenerateFromCapabilities() {
        Tournament tournament = registrationTournament();
        User organizer = user(UUID.randomUUID(), "organizer", "encoded", Role.USER);
        tournament.setOrganizer(organizer);
        tournament.setStatus(TournamentStatus.BRACKET_READY);
        for (int i = 0; i < 4; i++) {
            tournament
                    .getParticipants()
                    .add(participant(
                            tournament,
                            user(UUID.randomUUID(), "player" + i, "encoded", Role.USER),
                            UUID.randomUUID()));
        }

        TournamentDetailResponse response = mapper.toTournamentDetailResponse(tournament, organizer);

        assertThat(response.viewerCapabilities().canGenerateBracket()).isFalse();
        assertThat(response.viewerCapabilities().canRemoveParticipants()).isFalse();
    }

    @Test
    void toTournamentDetailResponse_organizerCapabilitiesDuringInProgress() {
        Tournament tournament = registrationTournament();
        User organizer = user(UUID.randomUUID(), "organizer", "encoded", Role.USER);
        tournament.setOrganizer(organizer);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        TournamentDetailResponse response = mapper.toTournamentDetailResponse(tournament, organizer);

        assertThat(response.viewerCapabilities().canManageTournament()).isTrue();
        assertThat(response.viewerCapabilities().canOverrideMatchResults()).isTrue();
        assertThat(response.viewerCapabilities().canGenerateBracket()).isFalse();
    }

    private Tournament registrationTournament() {
        return Tournament.builder()
                .id(UUID.randomUUID())
                .title("Tournament")
                .joinCode("ABC123")
                .participationMode(TournamentParticipationMode.SELF_JOIN)
                .status(TournamentStatus.REGISTRATION)
                .format(TournamentFormat.SINGLE_ELIMINATION)
                .gameType(GameType.SINGLES)
                .build();
    }

    private TournamentParticipant participant(Tournament tournament, User user, UUID id) {
        return TournamentParticipant.builder()
                .id(id)
                .tournament(tournament)
                .user(user)
                .build();
    }

    private Tournament tournament() {
        return Tournament.builder()
                .id(UUID.randomUUID())
                .title("Tournament")
                .joinCode("ABC123")
                .participationMode(TournamentParticipationMode.SELF_JOIN)
                .status(TournamentStatus.IN_PROGRESS)
                .format(TournamentFormat.DOUBLE_ELIMINATION)
                .build();
    }

    private TournamentRound round(Tournament tournament, BracketType bracketType) {
        return TournamentRound.builder()
                .tournament(tournament)
                .bracketType(bracketType)
                .roundNumber(1)
                .bestOf(1)
                .build();
    }

    private Match match(TournamentRound round, TournamentTeam teamOne, TournamentTeam teamTwo) {
        return Match.builder()
                .id(UUID.randomUUID())
                .round(round)
                .matchNumber(1)
                .teamOne(teamOne)
                .teamTwo(teamTwo)
                .status(MatchStatus.PENDING)
                .build();
    }

    private TournamentTeam team(Tournament tournament, String username) {
        return TournamentTeam.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .playerOne(TournamentParticipant.builder()
                        .id(UUID.randomUUID())
                        .tournament(tournament)
                        .user(user(UUID.randomUUID(), username, "encoded", Role.USER))
                        .build())
                .build();
    }
}
