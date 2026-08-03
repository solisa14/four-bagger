package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentMatchSupportTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentGameResultRepository resultRepository;

    @Mock
    private TournamentMapper tournamentMapper;

    @Mock
    private TournamentProgressionService progressionService;

    private TournamentMatchSupport matchSupport;

    @BeforeEach
    void setUp() {
        matchSupport = new TournamentMatchSupport(
                tournamentRepository, matchRepository, resultRepository, tournamentMapper, progressionService);
    }

    @Test
    void requireMatch_whenFound_returnsMatch() {
        Tournament tournament = tournament();
        Match match = match(tournament);
        when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));

        Match result = matchSupport.requireMatch(match.getId(), tournament.getId());

        assertThat(result).isEqualTo(match);
    }

    @Test
    void requireTournament_whenNotFound_throwsTournamentNotFoundException() {
        UUID tournamentId = UUID.randomUUID();
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchSupport.requireTournament(tournamentId))
                .isInstanceOf(TournamentNotFoundException.class);
    }

    @Test
    void requireMatch_whenMatchNotFound_throwsMatchNotFoundException() {
        Tournament tournament = tournament();
        UUID matchId = UUID.randomUUID();
        when(matchRepository.findForResponseById(matchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchSupport.requireMatch(matchId, tournament.getId()))
                .isInstanceOf(MatchNotFoundException.class);
    }

    private Tournament tournament() {
        return Tournament.builder()
                .id(UUID.randomUUID())
                .organizer(user("organizer"))
                .title("Tournament")
                .status(TournamentStatus.IN_PROGRESS)
                .joinCode("ABC123")
                .participationMode(TournamentParticipationMode.SELF_JOIN)
                .build();
    }

    private Match match(Tournament tournament) {
        TournamentRound round = TournamentRound.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .bracketType(BracketType.WINNERS)
                .roundNumber(1)
                .bestOf(1)
                .build();
        TournamentTeam teamOne = TournamentTeam.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .playerOne(TournamentParticipant.builder()
                        .id(UUID.randomUUID())
                        .tournament(tournament)
                        .user(user("p1"))
                        .build())
                .seed(1)
                .build();
        TournamentTeam teamTwo = TournamentTeam.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .playerOne(TournamentParticipant.builder()
                        .id(UUID.randomUUID())
                        .tournament(tournament)
                        .user(user("p2"))
                        .build())
                .seed(2)
                .build();
        return Match.builder()
                .id(UUID.randomUUID())
                .round(round)
                .matchNumber(1)
                .teamOne(teamOne)
                .teamTwo(teamTwo)
                .status(MatchStatus.PENDING)
                .build();
    }

    private User user(String username) {
        return TestDataFactory.user(UUID.randomUUID(), username, "encoded", Role.USER);
    }
}
