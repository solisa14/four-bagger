package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TournamentBracketEligibilityPolicyTest {

    private TournamentBracketEligibilityPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new TournamentBracketEligibilityPolicy();
    }

    @Test
    void singlesSingleElimination_twoParticipants_isIneligible() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.SINGLES, TournamentFormat.SINGLE_ELIMINATION, 2));

        assertThat(result.eligible()).isFalse();
        assertThat(result.participantCount()).isEqualTo(2);
        assertThat(result.minimumParticipantCount()).isEqualTo(3);
        assertThat(result.requiresEvenParticipantCount()).isFalse();
        assertThat(result.message()).isEqualTo("At least 3 participants are required.");
    }

    @Test
    void singlesSingleElimination_threeParticipants_isEligible() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.SINGLES, TournamentFormat.SINGLE_ELIMINATION, 3));

        assertThat(result.eligible()).isTrue();
        assertThat(result.minimumParticipantCount()).isEqualTo(3);
        assertThat(result.requiresEvenParticipantCount()).isFalse();
        assertThat(result.message()).isEqualTo("Participant requirements are met.");
    }

    @Test
    void singlesNullFormat_usesSingleEliminationMinimum() {
        BracketEligibility result = policy.evaluate(tournament(GameType.SINGLES, null, 3));

        assertThat(result.eligible()).isTrue();
        assertThat(result.minimumParticipantCount()).isEqualTo(3);
        assertThat(result.message()).isEqualTo("Participant requirements are met.");
    }

    @Test
    void singlesDoubleElimination_threeParticipants_isIneligible() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.SINGLES, TournamentFormat.DOUBLE_ELIMINATION, 3));

        assertThat(result.eligible()).isFalse();
        assertThat(result.minimumParticipantCount()).isEqualTo(4);
        assertThat(result.requiresEvenParticipantCount()).isFalse();
        assertThat(result.message()).isEqualTo("At least 4 participants are required.");
    }

    @Test
    void singlesDoubleElimination_fourParticipants_isEligible() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.SINGLES, TournamentFormat.DOUBLE_ELIMINATION, 4));

        assertThat(result.eligible()).isTrue();
        assertThat(result.minimumParticipantCount()).isEqualTo(4);
        assertThat(result.message()).isEqualTo("Participant requirements are met.");
    }

    @Test
    void doublesSingleElimination_fiveParticipants_isIneligibleDueToMinimum() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.DOUBLES, TournamentFormat.SINGLE_ELIMINATION, 5));

        assertThat(result.eligible()).isFalse();
        assertThat(result.minimumParticipantCount()).isEqualTo(6);
        assertThat(result.requiresEvenParticipantCount()).isTrue();
        assertThat(result.message()).isEqualTo("At least 6 participants are required.");
    }

    @Test
    void doublesSingleElimination_sixParticipants_isEligible() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.DOUBLES, TournamentFormat.SINGLE_ELIMINATION, 6));

        assertThat(result.eligible()).isTrue();
        assertThat(result.minimumParticipantCount()).isEqualTo(6);
        assertThat(result.requiresEvenParticipantCount()).isTrue();
        assertThat(result.message()).isEqualTo("Participant requirements are met.");
    }

    @Test
    void doublesNullFormat_usesSingleEliminationMinimum() {
        BracketEligibility result = policy.evaluate(tournament(GameType.DOUBLES, null, 6));

        assertThat(result.eligible()).isTrue();
        assertThat(result.minimumParticipantCount()).isEqualTo(6);
        assertThat(result.message()).isEqualTo("Participant requirements are met.");
    }

    @Test
    void doublesSingleElimination_sevenParticipants_isIneligibleDueToOddCount() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.DOUBLES, TournamentFormat.SINGLE_ELIMINATION, 7));

        assertThat(result.eligible()).isFalse();
        assertThat(result.minimumParticipantCount()).isEqualTo(6);
        assertThat(result.requiresEvenParticipantCount()).isTrue();
        assertThat(result.message()).isEqualTo("Doubles tournaments require an even number of participants.");
    }

    @Test
    void doublesSingleElimination_eightParticipants_isEligible() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.DOUBLES, TournamentFormat.SINGLE_ELIMINATION, 8));

        assertThat(result.eligible()).isTrue();
        assertThat(result.message()).isEqualTo("Participant requirements are met.");
    }

    @Test
    void doublesDoubleElimination_sixParticipants_isIneligibleDueToMinimum() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.DOUBLES, TournamentFormat.DOUBLE_ELIMINATION, 6));

        assertThat(result.eligible()).isFalse();
        assertThat(result.minimumParticipantCount()).isEqualTo(8);
        assertThat(result.message()).isEqualTo("At least 8 participants are required.");
    }

    @Test
    void doublesDoubleElimination_sevenParticipants_isIneligibleDueToMinimum() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.DOUBLES, TournamentFormat.DOUBLE_ELIMINATION, 7));

        assertThat(result.eligible()).isFalse();
        assertThat(result.minimumParticipantCount()).isEqualTo(8);
        assertThat(result.message()).isEqualTo("At least 8 participants are required.");
    }

    @Test
    void doublesDoubleElimination_eightParticipants_isEligible() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.DOUBLES, TournamentFormat.DOUBLE_ELIMINATION, 8));

        assertThat(result.eligible()).isTrue();
        assertThat(result.minimumParticipantCount()).isEqualTo(8);
        assertThat(result.message()).isEqualTo("Participant requirements are met.");
    }

    @Test
    void doublesDoubleElimination_nineParticipants_isIneligibleDueToOddCount() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.DOUBLES, TournamentFormat.DOUBLE_ELIMINATION, 9));

        assertThat(result.eligible()).isFalse();
        assertThat(result.minimumParticipantCount()).isEqualTo(8);
        assertThat(result.message()).isEqualTo("Doubles tournaments require an even number of participants.");
    }

    @Test
    void doublesDoubleElimination_tenParticipants_isEligible() {
        BracketEligibility result =
                policy.evaluate(tournament(GameType.DOUBLES, TournamentFormat.DOUBLE_ELIMINATION, 10));

        assertThat(result.eligible()).isTrue();
        assertThat(result.message()).isEqualTo("Participant requirements are met.");
    }

    @Test
    void manualDoubles_sixParticipantsWithoutTeams_isIneligible() {
        Tournament tournament = manualDoublesTournament(TournamentFormat.SINGLE_ELIMINATION, 6, false);

        BracketEligibility result = policy.evaluate(tournament);

        assertThat(result.eligible()).isFalse();
        assertThat(result.message()).isEqualTo("Every guest must belong to exactly one complete team.");
    }

    @Test
    void manualDoubles_sixParticipantsWithCompleteTeams_isEligible() {
        Tournament tournament = manualDoublesTournament(TournamentFormat.SINGLE_ELIMINATION, 6, true);

        BracketEligibility result = policy.evaluate(tournament);

        assertThat(result.eligible()).isTrue();
        assertThat(result.message()).isEqualTo("Participant requirements are met.");
    }

    private Tournament tournament(GameType gameType, TournamentFormat format, int participantCount) {
        Tournament tournament = Tournament.builder()
                .gameType(gameType)
                .format(format)
                .status(TournamentStatus.REGISTRATION)
                .build();
        for (int i = 0; i < participantCount; i++) {
            tournament
                    .getParticipants()
                    .add(TournamentParticipant.builder().tournament(tournament).build());
        }
        return tournament;
    }

    private Tournament manualDoublesTournament(TournamentFormat format, int participantCount, boolean completeTeams) {
        Tournament tournament = Tournament.builder()
                .gameType(GameType.DOUBLES)
                .format(format)
                .status(TournamentStatus.REGISTRATION)
                .participationMode(TournamentParticipationMode.ORGANIZER_MANAGED)
                .doublesPairingMode(DoublesPairingMode.MANUAL)
                .build();
        for (int i = 0; i < participantCount; i++) {
            tournament
                    .getParticipants()
                    .add(TournamentParticipant.builder()
                            .id(java.util.UUID.randomUUID())
                            .tournament(tournament)
                            .displayName("Guest " + i)
                            .build());
        }
        if (completeTeams) {
            for (int i = 0; i < participantCount; i += 2) {
                tournament
                        .getTeams()
                        .add(TournamentTeam.builder()
                                .tournament(tournament)
                                .playerOne(tournament.getParticipants().get(i))
                                .playerTwo(tournament.getParticipants().get(i + 1))
                                .build());
            }
        }
        return tournament;
    }
}
