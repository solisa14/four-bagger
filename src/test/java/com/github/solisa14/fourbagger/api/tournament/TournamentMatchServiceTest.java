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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentMatchServiceTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private MatchRepository matchRepository;
  @Mock private TournamentMatchAuthorizationService authorizationService;
  @Mock private TournamentMapper tournamentMapper;
  @Mock private TournamentGameResultRepository resultRepository;
  @Mock private TournamentProgressionService progressionService;

  @InjectMocks private TournamentMatchService tournamentMatchService;

  @Test
  void startMatch_whenTournamentNotInProgress_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament(TournamentStatus.BRACKET_READY);
    Match match = match(tournament, false);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () ->
                tournamentMatchService.startMatch(
                    tournament.getId(), match.getId(), tournament.getOrganizer()))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  @Test
  void startMatch_whenUserIsNotAuthorized_throwsTournamentAccessDeniedException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    doThrow(new TournamentAccessDeniedException(tournament.getId()))
        .when(authorizationService)
        .authorizeMatchMutation(any(), eq(tournament), eq(match));

    assertThatThrownBy(
            () ->
                tournamentMatchService.startMatch(
                    tournament.getId(), match.getId(), user("outsider")))
        .isInstanceOf(TournamentAccessDeniedException.class);
  }

  @Test
  void startMatch_whenPending_marksMatchInProgressAndReturnsDetail() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    User starter = tournament.getOrganizer();
    TournamentMatchDetailResponse detail = detailResponse(match);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId())).thenReturn(List.of());
    when(progressionService.nextGameNumber(match)).thenReturn(1);
    when(tournamentMapper.toMatchDetailResponse(eq(match), eq(List.of()), eq(1)))
        .thenReturn(detail);

    TournamentMatchDetailResponse result =
        tournamentMatchService.startMatch(tournament.getId(), match.getId(), starter);

    assertThat(result).isEqualTo(detail);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    assertThat(match.getStartedAt()).isNotNull();
    assertThat(match.getStartedBy()).isEqualTo(starter);
    verify(matchRepository).save(match);
  }

  @Test
  void startMatch_whenParticipantStarts_authorizesAndMarksInProgress() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    User participant = match.getTeamOne().getPlayerOne();
    TournamentMatchDetailResponse detail = detailResponse(match);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId())).thenReturn(List.of());
    when(progressionService.nextGameNumber(match)).thenReturn(1);
    when(tournamentMapper.toMatchDetailResponse(eq(match), eq(List.of()), eq(1)))
        .thenReturn(detail);

    tournamentMatchService.startMatch(tournament.getId(), match.getId(), participant);

    verify(authorizationService).authorizeMatchMutation(participant, tournament, match);
    verify(matchRepository).save(match);
  }

  @Test
  void startMatch_whenAlreadyInProgressWithStartedAt_isIdempotentAndDoesNotSaveAgain() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    Instant startedAt = Instant.parse("2026-01-01T12:00:00Z");
    match.setStatus(MatchStatus.IN_PROGRESS);
    match.setStartedAt(startedAt);
    match.setStartedBy(tournament.getOrganizer());
    TournamentMatchDetailResponse detail = detailResponse(match);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId())).thenReturn(List.of());
    when(progressionService.nextGameNumber(match)).thenReturn(1);
    when(tournamentMapper.toMatchDetailResponse(eq(match), eq(List.of()), eq(1)))
        .thenReturn(detail);

    TournamentMatchDetailResponse result =
        tournamentMatchService.startMatch(
            tournament.getId(), match.getId(), tournament.getOrganizer());

    assertThat(result).isEqualTo(detail);
    verify(matchRepository, never()).save(match);
  }

  @Test
  void startMatch_whenMatchIsBye_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.setBye(true);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () ->
                tournamentMatchService.startMatch(
                    tournament.getId(), match.getId(), tournament.getOrganizer()))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  @Test
  void startMatch_whenMatchTeamMissing_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.setTeamTwo(null);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () ->
                tournamentMatchService.startMatch(
                    tournament.getId(), match.getId(), tournament.getOrganizer()))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  @Test
  void startMatch_whenMatchBelongsToDifferentTournament_throwsInvalidTournamentStateException() {
    Tournament requestedTournament = tournament(TournamentStatus.IN_PROGRESS);
    Tournament ownerTournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(ownerTournament, false);
    when(tournamentRepository.findById(requestedTournament.getId()))
        .thenReturn(Optional.of(requestedTournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () ->
                tournamentMatchService.startMatch(
                    requestedTournament.getId(), match.getId(), requestedTournament.getOrganizer()))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  @Test
  void getMatchDetail_whenFound_returnsDetail() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    TournamentMatchDetailResponse detail = detailResponse(match);

    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));
    when(resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId())).thenReturn(List.of());
    when(progressionService.nextGameNumber(match)).thenReturn(1);
    when(tournamentMapper.toMatchDetailResponse(eq(match), eq(List.of()), eq(1)))
        .thenReturn(detail);

    TournamentMatchDetailResponse result =
        tournamentMatchService.getMatchDetail(tournament.getId(), match.getId());

    assertThat(result).isEqualTo(detail);
  }

  @Test
  void getMatch_whenFound_returnsMatch() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(match.getId())).thenReturn(Optional.of(match));

    Match result = tournamentMatchService.getMatch(tournament.getId(), match.getId());

    assertThat(result).isEqualTo(match);
  }

  @Test
  void getMatch_whenTournamentNotFound_throwsTournamentNotFoundException() {
    UUID tournamentId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();
    when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tournamentMatchService.getMatch(tournamentId, matchId))
        .isInstanceOf(TournamentNotFoundException.class);
  }

  @Test
  void getMatch_whenMatchNotFound_throwsMatchNotFoundException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    UUID matchId = UUID.randomUUID();
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findForResponseById(matchId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tournamentMatchService.getMatch(tournament.getId(), matchId))
        .isInstanceOf(MatchNotFoundException.class);
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
        match.getStartedAt(),
        match.getStartedBy() != null ? match.getStartedBy().getId() : null,
        match.getRound().getBestOf(),
        1,
        1,
        List.of());
  }

  private Tournament tournament(TournamentStatus status) {
    return Tournament.builder()
        .id(UUID.randomUUID())
        .organizer(user("organizer"))
        .title("Tournament")
        .status(status)
        .joinCode("ABC123")
        .build();
  }

  private Match match(Tournament tournament, boolean doubles) {
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
            .playerTwo(doubles ? user("team1-b") : null)
            .seed(1)
            .build();
    TournamentTeam teamTwo =
        TournamentTeam.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerOne(user("team2-a"))
            .playerTwo(doubles ? user("team2-b") : null)
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
