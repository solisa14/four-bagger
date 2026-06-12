package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentMatchResultServiceTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private MatchRepository matchRepository;
  @Mock private TournamentTeamRepository teamRepository;
  @Mock private TournamentGameResultRepository resultRepository;
  @Mock private TournamentMatchAuthorizationService authorizationService;
  @Mock private FinalScoreValidator finalScoreValidator;
  @Mock private TournamentProgressionService progressionService;
  @Mock private TournamentMapper tournamentMapper;

  @InjectMocks private TournamentMatchResultService tournamentMatchResultService;

  @Test
  void submitResult_whenNewResult_savesAndAppliesProgression() {
    Tournament tournament = tournament();
    Match match = match(tournament);
    match.setStatus(MatchStatus.IN_PROGRESS);
    User submitter = match.getTeamOne().getPlayerOne();
    SubmitTournamentGameResultRequest request =
        new SubmitTournamentGameResultRequest(match.getTeamOne().getId(), 21, 15);
    TournamentMatchDetailResponse detail = detailResponse(match);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(resultRepository.findByMatchIdAndGameNumber(match.getId(), 1)).thenReturn(Optional.empty());
    when(progressionService.nextGameNumber(match)).thenReturn(1, 2);
    when(teamRepository.findById(match.getTeamOne().getId()))
        .thenReturn(Optional.of(match.getTeamOne()));
    when(resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId())).thenReturn(List.of());
    when(tournamentMapper.toMatchDetailResponse(eq(match), any(), eq(2))).thenReturn(detail);

    TournamentMatchDetailResponse result =
        tournamentMatchResultService.submitResult(
            tournament.getId(), match.getId(), 1, submitter, request);

    assertThat(result).isEqualTo(detail);
    verify(resultRepository).saveAndFlush(any(TournamentGameResult.class));
    verify(progressionService).applyGameResult(any(TournamentGameResult.class));
    verify(finalScoreValidator).validateScores(21, 15);
  }

  @Test
  void submitResult_whenExactRetry_returnsDetailWithoutReapplyingProgression() {
    Tournament tournament = tournament();
    Match match = match(tournament);
    match.setStatus(MatchStatus.IN_PROGRESS);
    User submitter = match.getTeamOne().getPlayerOne();
    TournamentGameResult existing =
        TournamentGameResult.builder()
            .id(UUID.randomUUID())
            .match(match)
            .gameNumber(1)
            .winnerTeam(match.getTeamOne())
            .teamOneScore(21)
            .teamTwoScore(15)
            .submittedBy(submitter)
            .submittedAt(Instant.now())
            .build();
    SubmitTournamentGameResultRequest request =
        new SubmitTournamentGameResultRequest(match.getTeamOne().getId(), 21, 15);
    TournamentMatchDetailResponse detail = detailResponse(match);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(resultRepository.findByMatchIdAndGameNumber(match.getId(), 1))
        .thenReturn(Optional.of(existing));
    when(resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId()))
        .thenReturn(List.of(existing));
    when(progressionService.nextGameNumber(match)).thenReturn(2);
    when(tournamentMapper.toMatchDetailResponse(eq(match), eq(List.of(existing)), eq(2)))
        .thenReturn(detail);

    TournamentMatchDetailResponse result =
        tournamentMatchResultService.submitResult(
            tournament.getId(), match.getId(), 1, submitter, request);

    assertThat(result).isEqualTo(detail);
    verify(resultRepository, never()).saveAndFlush(any());
    verify(progressionService, never()).applyGameResult(any());
  }

  @Test
  void submitResult_whenConflictingRetry_throwsResultAlreadySubmittedException() {
    Tournament tournament = tournament();
    Match match = match(tournament);
    match.setStatus(MatchStatus.IN_PROGRESS);
    User submitter = match.getTeamOne().getPlayerOne();
    TournamentGameResult existing =
        TournamentGameResult.builder()
            .id(UUID.randomUUID())
            .match(match)
            .gameNumber(1)
            .winnerTeam(match.getTeamOne())
            .teamOneScore(21)
            .teamTwoScore(15)
            .submittedBy(submitter)
            .submittedAt(Instant.now())
            .build();
    SubmitTournamentGameResultRequest request =
        new SubmitTournamentGameResultRequest(match.getTeamTwo().getId(), 18, 21);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(resultRepository.findByMatchIdAndGameNumber(match.getId(), 1))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () ->
                tournamentMatchResultService.submitResult(
                    tournament.getId(), match.getId(), 1, submitter, request))
        .isInstanceOf(ResultAlreadySubmittedException.class);

    verify(progressionService, never()).applyGameResult(any());
  }

  @Test
  void submitResult_whenDuplicateFlushFails_throwsResultAlreadySubmittedWithoutProgression() {
    Tournament tournament = tournament();
    Match match = match(tournament);
    match.setStatus(MatchStatus.IN_PROGRESS);
    User submitter = match.getTeamOne().getPlayerOne();
    SubmitTournamentGameResultRequest request =
        new SubmitTournamentGameResultRequest(match.getTeamOne().getId(), 21, 15);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(resultRepository.findByMatchIdAndGameNumber(match.getId(), 1)).thenReturn(Optional.empty());
    when(progressionService.nextGameNumber(match)).thenReturn(1);
    when(teamRepository.findById(match.getTeamOne().getId()))
        .thenReturn(Optional.of(match.getTeamOne()));
    doThrow(new DataIntegrityViolationException("duplicate"))
        .when(resultRepository)
        .saveAndFlush(any(TournamentGameResult.class));

    assertThatThrownBy(
            () ->
                tournamentMatchResultService.submitResult(
                    tournament.getId(), match.getId(), 1, submitter, request))
        .isInstanceOf(ResultAlreadySubmittedException.class);

    verify(progressionService, never()).applyGameResult(any());
  }

  @Test
  void submitResult_whenOutOfSequenceGameNumber_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament();
    Match match = match(tournament);
    match.setStatus(MatchStatus.IN_PROGRESS);
    User submitter = match.getTeamOne().getPlayerOne();
    SubmitTournamentGameResultRequest request =
        new SubmitTournamentGameResultRequest(match.getTeamOne().getId(), 21, 15);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(resultRepository.findByMatchIdAndGameNumber(match.getId(), 2)).thenReturn(Optional.empty());
    when(progressionService.nextGameNumber(match)).thenReturn(1);

    assertThatThrownBy(
            () ->
                tournamentMatchResultService.submitResult(
                    tournament.getId(), match.getId(), 2, submitter, request))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  private TournamentMatchDetailResponse detailResponse(Match match) {
    return new TournamentMatchDetailResponse(
        match.getId(),
        match.getMatchNumber(),
        match.getStatus(),
        match.isBye(),
        null,
        null,
        match.getTeamOneWins(),
        match.getTeamTwoWins(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        match.getRound().getBestOf(),
        1,
        2,
        List.of());
  }

  private Tournament tournament() {
    return Tournament.builder()
        .id(UUID.randomUUID())
        .organizer(user("organizer"))
        .title("Tournament")
        .status(TournamentStatus.IN_PROGRESS)
        .joinCode("ABC123")
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
            .playerOne(user("team1-a"))
            .seed(1)
            .build();
    TournamentTeam teamTwo =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(user("team2-a"))
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

  private User user(String suffix) {
    return TestDataFactory.user(UUID.randomUUID(), suffix, "encoded", Role.USER);
  }
}
