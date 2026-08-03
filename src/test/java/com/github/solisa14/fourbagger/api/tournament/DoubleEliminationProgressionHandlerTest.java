package com.github.solisa14.fourbagger.api.tournament;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoubleEliminationProgressionHandlerTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchRepository matchRepository;

    private DoubleEliminationProgressionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DoubleEliminationProgressionHandler(
                tournamentRepository, matchRepository, new DoubleEliminationByeResolver());
    }

    @Test
    void progress_routesWinnerAndFirstTimeLoserWithoutEliminatingLoser() {
        Tournament tournament = tournament();
        TournamentTeam winner = team(tournament);
        TournamentTeam loser = team(tournament);
        Match winnerDestination = match(tournament, null, null);
        Match loserDestination = match(tournament, null, null);
        Match source = match(tournament, winner, loser);
        source.setWinnerNextMatch(winnerDestination);
        source.setWinnerNextMatchPosition(2);
        source.setLoserNextMatch(loserDestination);
        source.setLoserNextMatchPosition(1);
        stubTournamentMatches(tournament, source, winnerDestination, loserDestination);

        handler.progress(source, winner, loser);

        assertThat(winnerDestination.getTeamTwo()).isSameAs(winner);
        assertThat(loserDestination.getTeamOne()).isSameAs(loser);
        assertThat(loser.getLosses()).isEqualTo(1);
        assertThat(loser.isEliminated()).isFalse();
        verify(matchRepository).save(winnerDestination);
        verify(matchRepository).save(loserDestination);
        verify(tournamentRepository, never()).save(tournament);
    }

    @Test
    void progress_whenLoserAlreadyHasOneLoss_marksLoserEliminated() {
        Tournament tournament = tournament();
        TournamentTeam winner = team(tournament);
        TournamentTeam loser = team(tournament);
        loser.setLosses(1);
        Match winnerDestination = match(tournament, null, null);
        Match source = match(tournament, winner, loser);
        source.setWinnerNextMatch(winnerDestination);
        source.setWinnerNextMatchPosition(1);
        stubTournamentMatches(tournament, source, winnerDestination);

        handler.progress(source, winner, loser);

        assertThat(loser.getLosses()).isEqualTo(2);
        assertThat(loser.isEliminated()).isTrue();
        assertThat(winnerDestination.getTeamOne()).isSameAs(winner);
    }

    @Test
    void progress_whenMatchIsTerminal_completesTournament() {
        Tournament tournament = tournament();
        TournamentTeam winner = team(tournament);
        TournamentTeam loser = team(tournament);
        loser.setLosses(1);
        Match source = match(tournament, winner, loser);
        stubTournamentMatches(tournament, source);

        handler.progress(source, winner, loser);

        assertThat(loser.getLosses()).isEqualTo(2);
        assertThat(loser.isEliminated()).isTrue();
        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        verify(tournamentRepository).save(tournament);
    }

    @Test
    void progress_whenUndefeatedFinalistWinsFirstFinal_completesWithoutActivatingReset() {
        Tournament tournament = tournament();
        TournamentTeam undefeatedWinner = team(tournament);
        TournamentTeam oneLossLoser = team(tournament);
        oneLossLoser.setLosses(1);
        Match reset = match(tournament, BracketType.GRAND_FINAL, null, null);
        Match firstFinal = match(tournament, BracketType.FINAL, undefeatedWinner, oneLossLoser);
        firstFinal.setWinnerNextMatch(reset);
        firstFinal.setWinnerNextMatchPosition(2);
        firstFinal.setLoserNextMatch(reset);
        firstFinal.setLoserNextMatchPosition(1);
        stubTournamentMatches(tournament, firstFinal, reset);

        handler.progress(firstFinal, undefeatedWinner, oneLossLoser);

        assertThat(oneLossLoser.getLosses()).isEqualTo(2);
        assertThat(oneLossLoser.isEliminated()).isTrue();
        assertThat(reset.getTeamOne()).isNull();
        assertThat(reset.getTeamTwo()).isNull();
        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        verify(matchRepository, never()).save(reset);
        verify(tournamentRepository).save(tournament);
    }

    @Test
    void progress_whenOneLossFinalistWinsFirstFinal_activatesResetAndCopiesBestOf() {
        Tournament tournament = tournament();
        TournamentTeam oneLossWinner = team(tournament);
        oneLossWinner.setLosses(1);
        TournamentTeam undefeatedLoser = team(tournament);
        Match reset = match(tournament, BracketType.GRAND_FINAL, null, null);
        Match firstFinal = match(tournament, BracketType.FINAL, undefeatedLoser, oneLossWinner);
        firstFinal.getRound().setBestOf(5);
        firstFinal.setWinnerNextMatch(reset);
        firstFinal.setWinnerNextMatchPosition(2);
        firstFinal.setLoserNextMatch(reset);
        firstFinal.setLoserNextMatchPosition(1);
        stubTournamentMatches(tournament, firstFinal, reset);

        handler.progress(firstFinal, oneLossWinner, undefeatedLoser);

        assertThat(undefeatedLoser.getLosses()).isEqualTo(1);
        assertThat(undefeatedLoser.isEliminated()).isFalse();
        assertThat(reset.getTeamOne()).isSameAs(undefeatedLoser);
        assertThat(reset.getTeamTwo()).isSameAs(oneLossWinner);
        assertThat(reset.getRound().getBestOf()).isEqualTo(5);
        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
        verify(matchRepository).save(reset);
        verify(tournamentRepository, never()).save(tournament);
    }

    @Test
    void progress_whenLoserEntersOneTeamLosersMatch_autoCompletesByeAndRoutesWinner() {
        Tournament tournament = tournament();
        TournamentTeam winner = team(tournament);
        TournamentTeam loser = team(tournament);
        Match winnerDestination = match(tournament, null, null);
        Match losersNext = match(tournament, null, null);
        Match completedFeeder = match(tournament, BracketType.WINNERS, team(tournament), null);
        completedFeeder.setBye(true);
        completedFeeder.setStatus(MatchStatus.COMPLETED);
        completedFeeder.setWinner(completedFeeder.getTeamOne());
        Match losersMatch = match(tournament, BracketType.LOSERS, null, null);
        losersMatch.setStatus(MatchStatus.PENDING);
        Match source = match(tournament, winner, loser);
        source.setStatus(MatchStatus.COMPLETED);
        source.setWinnerNextMatch(winnerDestination);
        source.setWinnerNextMatchPosition(1);
        source.setLoserNextMatch(losersMatch);
        source.setLoserNextMatchPosition(2);
        completedFeeder.setLoserNextMatch(losersMatch);
        completedFeeder.setLoserNextMatchPosition(1);
        losersMatch.setWinnerNextMatch(losersNext);
        losersMatch.setWinnerNextMatchPosition(1);
        stubTournamentMatches(tournament, completedFeeder, source, losersMatch, winnerDestination, losersNext);

        handler.progress(source, winner, loser);

        assertThat(losersMatch.getTeamTwo()).isSameAs(loser);
        assertThat(losersMatch.isBye()).isTrue();
        assertThat(losersMatch.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(losersMatch.getWinner()).isSameAs(loser);
        assertThat(losersNext.getTeamOne()).isSameAs(loser);
        verify(matchRepository, atLeastOnce()).save(losersMatch);
        verify(matchRepository, atLeastOnce()).save(losersNext);
    }

    @Test
    void revert_whenRuntimeByeWasAutoAdvanced_resetsByeAndClearsDownstreamSlot() {
        Tournament tournament = tournament();
        TournamentTeam winner = team(tournament);
        TournamentTeam loser = team(tournament);
        Match winnerDestination = match(tournament, null, null);
        winnerDestination.setTeamOne(winner);
        Match losersNext = match(tournament, null, null);
        losersNext.setTeamOne(loser);
        Match completedFeeder = match(tournament, BracketType.WINNERS, team(tournament), null);
        completedFeeder.setBye(true);
        completedFeeder.setStatus(MatchStatus.COMPLETED);
        completedFeeder.setWinner(completedFeeder.getTeamOne());
        Match losersMatch = match(tournament, BracketType.LOSERS, null, loser);
        losersMatch.setBye(true);
        losersMatch.setStatus(MatchStatus.COMPLETED);
        losersMatch.setWinner(loser);
        losersMatch.setWinnerNextMatch(losersNext);
        losersMatch.setWinnerNextMatchPosition(1);
        Match source = match(tournament, winner, loser);
        source.setStatus(MatchStatus.COMPLETED);
        source.setWinnerNextMatch(winnerDestination);
        source.setWinnerNextMatchPosition(1);
        source.setLoserNextMatch(losersMatch);
        source.setLoserNextMatchPosition(2);
        completedFeeder.setLoserNextMatch(losersMatch);
        completedFeeder.setLoserNextMatchPosition(1);
        loser.setLosses(1);
        stubTournamentMatches(tournament, completedFeeder, source, losersMatch, winnerDestination, losersNext);

        handler.revert(source, winner, loser);

        assertThat(winnerDestination.getTeamOne()).isNull();
        assertThat(losersMatch.getTeamTwo()).isNull();
        assertThat(losersMatch.isBye()).isFalse();
        assertThat(losersMatch.getStatus()).isEqualTo(MatchStatus.PENDING);
        assertThat(losersMatch.getWinner()).isNull();
        assertThat(losersNext.getTeamOne()).isNull();
        assertThat(loser.getLosses()).isZero();
        verify(matchRepository, atLeastOnce()).save(losersMatch);
        verify(matchRepository, atLeastOnce()).save(losersNext);
    }

    @Test
    void revert_routesWinnerAndFirstTimeLoserWithoutEliminatingLoser() {
        Tournament tournament = tournament();
        TournamentTeam winner = team(tournament);
        TournamentTeam loser = team(tournament);
        Match winnerDestination = match(tournament, null, null);
        winnerDestination.setTeamTwo(winner);
        Match loserDestination = match(tournament, null, null);
        loserDestination.setTeamOne(loser);
        Match source = match(tournament, winner, loser);
        source.setWinnerNextMatch(winnerDestination);
        source.setWinnerNextMatchPosition(2);
        source.setLoserNextMatch(loserDestination);
        source.setLoserNextMatchPosition(1);
        loser.setLosses(1);
        stubTournamentMatches(tournament, source, winnerDestination, loserDestination);

        handler.revert(source, winner, loser);

        assertThat(winnerDestination.getTeamTwo()).isNull();
        assertThat(loserDestination.getTeamOne()).isNull();
        assertThat(loser.getLosses()).isZero();
        assertThat(loser.isEliminated()).isFalse();
        verify(matchRepository).save(winnerDestination);
        verify(matchRepository).save(loserDestination);
    }

    @Test
    void revert_whenLoserHadSecondLoss_restoresEliminationState() {
        Tournament tournament = tournament();
        TournamentTeam winner = team(tournament);
        TournamentTeam loser = team(tournament);
        loser.setLosses(2);
        loser.setEliminated(true);
        Match source = match(tournament, winner, loser);
        stubTournamentMatches(tournament, source);

        handler.revert(source, winner, loser);

        assertThat(loser.getLosses()).isEqualTo(1);
        assertThat(loser.isEliminated()).isFalse();
    }

    private void stubTournamentMatches(Tournament tournament, Match... matches) {
        List<Match> matchList = new ArrayList<>(List.of(matches));
        when(matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(tournament.getId()))
                .thenReturn(matchList);
    }

    private Tournament tournament() {
        return Tournament.builder()
                .id(UUID.randomUUID())
                .status(TournamentStatus.IN_PROGRESS)
                .format(TournamentFormat.DOUBLE_ELIMINATION)
                .build();
    }

    private TournamentTeam team(Tournament tournament) {
        return TournamentTeam.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .build();
    }

    private Match match(Tournament tournament, TournamentTeam teamOne, TournamentTeam teamTwo) {
        return match(tournament, BracketType.WINNERS, teamOne, teamTwo);
    }

    private Match match(
            Tournament tournament, BracketType bracketType, TournamentTeam teamOne, TournamentTeam teamTwo) {
        TournamentRound round = TournamentRound.builder()
                .tournament(tournament)
                .bracketType(bracketType)
                .roundNumber(1)
                .bestOf(1)
                .build();
        return Match.builder()
                .id(UUID.randomUUID())
                .round(round)
                .teamOne(teamOne)
                .teamTwo(teamTwo)
                .matchNumber(1)
                .status(MatchStatus.PENDING)
                .build();
    }
}
