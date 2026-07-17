package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentMatchOverrideServiceTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private MatchRepository matchRepository;
  @Mock private TournamentGameResultRepository resultRepository;
  @Mock private TournamentMatchAuthorizationService authorizationService;
  @Mock private TournamentProgressionService progressionService;
  @Mock private TournamentMapper tournamentMapper;

  @InjectMocks private TournamentMatchOverrideService tournamentMatchOverrideService;

  @Test
  void overrideMatchResult_whenOrganizerAndCompletedMatch_revertsAndAppliesCanonicalScores() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    User organizer = tournament.getOrganizer();
    Match match = completedMatch(tournament);
    OverrideTournamentMatchResultRequest request =
        new OverrideTournamentMatchResultRequest(match.getTeamTwo().getId());
    TournamentMatchDetailResponse detail = detailResponse(match);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(progressionService.winsToClinch(match)).thenReturn(1);
    when(progressionService.nextGameNumber(match)).thenReturn(null);
    when(resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId())).thenReturn(List.of());
    when(tournamentMapper.toMatchDetailResponse(eq(match), any(), eq(null))).thenReturn(detail);

    TournamentMatchDetailResponse result =
        tournamentMatchOverrideService.overrideMatchResult(
            tournament.getId(), match.getId(), organizer, request);

    assertThat(result).isEqualTo(detail);
    verify(authorizationService).authorizeOrganizer(organizer, tournament);
    verify(progressionService).revertMatchCompletion(match);
    verify(resultRepository).deleteByMatchId(match.getId());
    verify(progressionService)
        .applyMatchOverride(match, match.getTeamTwo(), match.getTeamOne(), 0, 1);
  }

  @Test
  void overrideMatchResult_whenBestOfFive_derivesThreeZeroCanonicalScore() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    User organizer = tournament.getOrganizer();
    Match match = match(tournament);
    match.getRound().setBestOf(5);
    match.setStatus(MatchStatus.IN_PROGRESS);
    OverrideTournamentMatchResultRequest request =
        new OverrideTournamentMatchResultRequest(match.getTeamOne().getId());
    TournamentMatchDetailResponse detail = detailResponse(match);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(progressionService.winsToClinch(match)).thenReturn(3);
    when(progressionService.nextGameNumber(match)).thenReturn(null);
    when(resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId())).thenReturn(List.of());
    when(tournamentMapper.toMatchDetailResponse(eq(match), any(), eq(null))).thenReturn(detail);

    tournamentMatchOverrideService.overrideMatchResult(
        tournament.getId(), match.getId(), organizer, request);

    verify(progressionService)
        .applyMatchOverride(match, match.getTeamOne(), match.getTeamTwo(), 3, 0);
  }

  @Test
  void overrideMatchResult_whenPendingMatch_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    User organizer = tournament.getOrganizer();
    Match match = match(tournament);
    OverrideTournamentMatchResultRequest request =
        new OverrideTournamentMatchResultRequest(match.getTeamOne().getId());

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () ->
                tournamentMatchOverrideService.overrideMatchResult(
                    tournament.getId(), match.getId(), organizer, request))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("IN_PROGRESS or COMPLETED");

    verify(resultRepository, never()).deleteByMatchId(any());
    verify(progressionService, never()).revertMatchCompletion(match);
    verify(progressionService, never()).applyMatchOverride(any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void overrideMatchResult_whenInProgressMatch_skipsRevert() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    User organizer = tournament.getOrganizer();
    Match match = match(tournament);
    match.setStatus(MatchStatus.IN_PROGRESS);
    OverrideTournamentMatchResultRequest request =
        new OverrideTournamentMatchResultRequest(match.getTeamOne().getId());
    TournamentMatchDetailResponse detail = detailResponse(match);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(progressionService.winsToClinch(match)).thenReturn(1);
    when(progressionService.nextGameNumber(match)).thenReturn(null);
    when(resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId())).thenReturn(List.of());
    when(tournamentMapper.toMatchDetailResponse(eq(match), any(), eq(null))).thenReturn(detail);

    tournamentMatchOverrideService.overrideMatchResult(
        tournament.getId(), match.getId(), organizer, request);

    verify(progressionService, never()).revertMatchCompletion(match);
    verify(progressionService)
        .applyMatchOverride(match, match.getTeamOne(), match.getTeamTwo(), 1, 0);
  }

  @Test
  void overrideMatchResult_whenDownstreamStarted_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    User organizer = tournament.getOrganizer();
    Match match = completedMatch(tournament);
    Match downstream = destinationMatch(tournament);
    downstream.setStatus(MatchStatus.IN_PROGRESS);
    match.setWinnerNextMatch(downstream);
    OverrideTournamentMatchResultRequest request =
        new OverrideTournamentMatchResultRequest(match.getTeamTwo().getId());

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () ->
                tournamentMatchOverrideService.overrideMatchResult(
                    tournament.getId(), match.getId(), organizer, request))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("downstream play has started");

    verify(progressionService, never()).revertMatchCompletion(match);
    verify(progressionService, never()).applyMatchOverride(any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void overrideMatchResult_whenInvalidWinner_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    User organizer = tournament.getOrganizer();
    Match match = match(tournament);
    match.setStatus(MatchStatus.IN_PROGRESS);
    OverrideTournamentMatchResultRequest request =
        new OverrideTournamentMatchResultRequest(UUID.randomUUID());

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () ->
                tournamentMatchOverrideService.overrideMatchResult(
                    tournament.getId(), match.getId(), organizer, request))
        .isInstanceOf(InvalidTournamentStateException.class)
        .hasMessageContaining("Winner team is not a participant in the match");
  }

  @Test
  void overrideMatchResult_whenNotOrganizer_throwsAccessDenied() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament);
    User participant = match.getTeamOne().getPlayerOne().getUser();
    OverrideTournamentMatchResultRequest request =
        new OverrideTournamentMatchResultRequest(match.getTeamOne().getId());

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    doThrow(new TournamentAccessDeniedException(tournament.getId()))
        .when(authorizationService)
        .authorizeOrganizer(participant, tournament);

    assertThatThrownBy(
            () ->
                tournamentMatchOverrideService.overrideMatchResult(
                    tournament.getId(), match.getId(), participant, request))
        .isInstanceOf(TournamentAccessDeniedException.class);
  }

  private TournamentMatchDetailResponse detailResponse(Match match) {
    return new TournamentMatchDetailResponse(
        match.getId(),
        match.getMatchNumber(),
        MatchStatus.COMPLETED,
        match.isBye(),
        null,
        null,
        0,
        1,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        match.getRound().getBestOf(),
        1,
        null,
        List.of());
  }

  private Tournament tournament(TournamentStatus status) {
    return Tournament.builder()
        .id(UUID.randomUUID())
        .organizer(user("organizer"))
        .title("Tournament")
        .status(status)
        .joinCode("ABC123")
        .participationMode(TournamentParticipationMode.SELF_JOIN)
        .build();
  }

  private Match match(Tournament tournament) {
    TournamentRound round =
        TournamentRound.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .bracketType(BracketType.WINNERS)
            .roundNumber(1)
            .bestOf(1)
            .build();
    TournamentTeam teamOne =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(participant(tournament, "team1-a"))
            .seed(1)
            .build();
    TournamentTeam teamTwo =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(participant(tournament, "team2-a"))
            .seed(2)
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

  private Match completedMatch(Tournament tournament) {
    Match match = match(tournament);
    match.setStatus(MatchStatus.COMPLETED);
    match.setWinner(match.getTeamOne());
    match.setTeamOneWins(1);
    match.setTeamTwoWins(0);
    return match;
  }

  private Match destinationMatch(Tournament tournament) {
    Match match = match(tournament);
    match.setStartedAt(Instant.now());
    return match;
  }


  private TournamentParticipant participant(Tournament tournament, String suffix) {
    return TournamentParticipant.builder()
        .id(UUID.randomUUID())
        .tournament(tournament)
        .user(user(suffix))
        .build();
  }

  private User user(String suffix) {
    return TestDataFactory.user(UUID.randomUUID(), suffix, "encoded", Role.USER);
  }
}
