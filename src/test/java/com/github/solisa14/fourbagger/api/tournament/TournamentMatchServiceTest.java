package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.solisa14.fourbagger.api.game.CreateGameCommand;
import com.github.solisa14.fourbagger.api.game.Game;
import com.github.solisa14.fourbagger.api.game.GameCompletedEvent;
import com.github.solisa14.fourbagger.api.game.GameCreationService;
import com.github.solisa14.fourbagger.api.game.GameRepository;
import com.github.solisa14.fourbagger.api.game.GameScoringMode;
import com.github.solisa14.fourbagger.api.game.GameStatus;
import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentMatchServiceTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private MatchRepository matchRepository;
  @Mock private GameRepository gameRepository;
  @Mock private GameCreationService gameCreationService;

  @InjectMocks private TournamentMatchService tournamentMatchService;

  @Test
  void startMatch_whenTournamentNotInProgress_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament(TournamentStatus.BRACKET_READY);
    Match match = match(tournament, false);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));

    assertThatThrownBy(
            () -> tournamentMatchService.startMatch(tournament.getId(), match.getId(), tournament.getOrganizer()))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  @Test
  void startMatch_whenUserIsNotOrganizer_throwsTournamentAccessDeniedException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));

    assertThatThrownBy(
            () -> tournamentMatchService.startMatch(tournament.getId(), match.getId(), user("outsider")))
        .isInstanceOf(TournamentAccessDeniedException.class);
  }

  @Test
  void startMatch_whenNoGamesExist_createsFirstPendingGameAndMarksMatchInProgress() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameRepository.findByTournamentMatchIdOrderByCreatedAtAsc(match.getId()))
        .thenReturn(List.of());
    Game created = Game.builder().id(UUID.randomUUID()).status(GameStatus.PENDING).build();
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class))).thenReturn(created);

    Game result =
        tournamentMatchService.startMatch(tournament.getId(), match.getId(), tournament.getOrganizer());

    assertThat(result).isEqualTo(created);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    verify(matchRepository).save(match);
    ArgumentCaptor<CreateGameCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateGameCommand.class);
    verify(gameCreationService).createPendingGame(commandCaptor.capture());
    CreateGameCommand command = commandCaptor.getValue();
    assertThat(command.tournamentMatchId()).isEqualTo(match.getId());
    assertThat(command.participants().gameType()).isEqualTo(GameType.SINGLES);
    assertThat(command.participants().teamOne().player())
        .isEqualTo(match.getTeamOne().getPlayerOne());
    assertThat(command.participants().teamTwo().player())
        .isEqualTo(match.getTeamTwo().getPlayerOne());
  }

  @Test
  void startMatch_whenTeamsAreDoubles_createsDoublesGameParticipants() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, true);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameRepository.findByTournamentMatchIdOrderByCreatedAtAsc(match.getId()))
        .thenReturn(List.of());
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenReturn(Game.builder().id(UUID.randomUUID()).status(GameStatus.PENDING).build());

    tournamentMatchService.startMatch(tournament.getId(), match.getId(), tournament.getOrganizer());

    ArgumentCaptor<CreateGameCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateGameCommand.class);
    verify(gameCreationService).createPendingGame(commandCaptor.capture());
    assertThat(commandCaptor.getValue().participants().gameType()).isEqualTo(GameType.DOUBLES);
  }

  @Test
  void startMatch_whenRoundUsesExactScoring_mapsToExactGameScoringMode() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().setScoringMode(ScoringMode.EXACT);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameRepository.findByTournamentMatchIdOrderByCreatedAtAsc(match.getId()))
        .thenReturn(List.of());
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenReturn(Game.builder().id(UUID.randomUUID()).status(GameStatus.PENDING).build());

    tournamentMatchService.startMatch(tournament.getId(), match.getId(), tournament.getOrganizer());

    ArgumentCaptor<CreateGameCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateGameCommand.class);
    verify(gameCreationService).createPendingGame(commandCaptor.capture());
    assertThat(commandCaptor.getValue().resolvedScoringMode()).isEqualTo(GameScoringMode.EXACT);
  }

  @Test
  void startMatch_whenGameAlreadyExists_returnsExistingGameWithoutCreatingAnother() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    Game existingGame = Game.builder().id(UUID.randomUUID()).build();
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameRepository.findByTournamentMatchIdOrderByCreatedAtAsc(match.getId()))
        .thenReturn(List.of(existingGame));

    Game result =
        tournamentMatchService.startMatch(tournament.getId(), match.getId(), tournament.getOrganizer());

    assertThat(result).isEqualTo(existingGame);
    verify(gameCreationService, never()).createPendingGame(any(CreateGameCommand.class));
  }

  @Test
  void startMatch_whenMatchIsBye_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.setBye(true);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () -> tournamentMatchService.startMatch(tournament.getId(), match.getId(), tournament.getOrganizer()))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  @Test
  void startMatch_whenMatchTeamMissing_throwsInvalidTournamentStateException() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.setTeamTwo(null);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () -> tournamentMatchService.startMatch(tournament.getId(), match.getId(), tournament.getOrganizer()))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  @Test
  void startMatch_whenMatchBelongsToDifferentTournament_throwsInvalidTournamentStateException() {
    Tournament requestedTournament = tournament(TournamentStatus.IN_PROGRESS);
    Tournament ownerTournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(ownerTournament, false);
    when(tournamentRepository.findById(requestedTournament.getId()))
        .thenReturn(Optional.of(requestedTournament));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(
            () ->
                tournamentMatchService.startMatch(
                    requestedTournament.getId(), match.getId(), requestedTournament.getOrganizer()))
        .isInstanceOf(InvalidTournamentStateException.class);
  }

  @Test
  void processCompletedGame_whenSeriesNotDecided_incrementsWinsAndCreatesNextGame() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().setBestOf(3);
    match.setStatus(MatchStatus.IN_PROGRESS);

    Game completedGame =
        Game.builder()
            .id(UUID.randomUUID())
            .status(GameStatus.COMPLETED)
            .winner(match.getTeamOne().getPlayerOne())
            .tournamentMatchId(match.getId())
            .build();
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenReturn(Game.builder().id(UUID.randomUUID()).build());

    tournamentMatchService.processCompletedGame(completedGame.getId());

    assertThat(match.getTeamOneWins()).isEqualTo(1);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    verify(gameCreationService).createPendingGame(any(CreateGameCommand.class));
    verify(matchRepository).save(match);
  }

  @Test
  void processCompletedGame_whenBestOfFive_requiresThreeWinsToClinch() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().setBestOf(5);
    match.setStatus(MatchStatus.IN_PROGRESS);
    match.setTeamOneWins(1);

    Game completedGame =
        Game.builder()
            .id(UUID.randomUUID())
            .status(GameStatus.COMPLETED)
            .winner(match.getTeamOne().getPlayerOne())
            .tournamentMatchId(match.getId())
            .build();
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenReturn(Game.builder().id(UUID.randomUUID()).build());

    tournamentMatchService.processCompletedGame(completedGame.getId());

    assertThat(match.getTeamOneWins()).isEqualTo(2);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    verify(gameCreationService).createPendingGame(any(CreateGameCommand.class));
  }

  @Test
  void processCompletedGame_whenRoundUsesExactScoring_nextGameUsesExactScoringMode() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().setBestOf(3);
    match.getRound().setScoringMode(ScoringMode.EXACT);
    match.setStatus(MatchStatus.IN_PROGRESS);

    Game completedGame =
        Game.builder()
            .id(UUID.randomUUID())
            .status(GameStatus.COMPLETED)
            .winner(match.getTeamOne().getPlayerOne())
            .tournamentMatchId(match.getId())
            .build();
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenReturn(Game.builder().id(UUID.randomUUID()).build());

    tournamentMatchService.processCompletedGame(completedGame.getId());

    ArgumentCaptor<CreateGameCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateGameCommand.class);
    verify(gameCreationService).createPendingGame(commandCaptor.capture());
    assertThat(commandCaptor.getValue().resolvedScoringMode()).isEqualTo(GameScoringMode.EXACT);
  }

  @Test
  void processCompletedGame_whenSeriesClinched_advancesWinnerToNextMatch() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    Match nextMatch = match(tournament, false);
    nextMatch.setTeamOne(null);
    nextMatch.setTeamTwo(null);
    match.setNextMatch(nextMatch);
    match.setNextMatchPosition(2);
    match.getRound().setBestOf(3);
    match.setTeamOneWins(1);

    Game completedGame =
        Game.builder()
            .id(UUID.randomUUID())
            .status(GameStatus.COMPLETED)
            .winner(match.getTeamOne().getPlayerOne())
            .tournamentMatchId(match.getId())
            .build();
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    tournamentMatchService.processCompletedGame(completedGame.getId());

    assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(match.getWinner()).isEqualTo(match.getTeamOne());
    assertThat(nextMatch.getTeamTwo()).isEqualTo(match.getTeamOne());
    verify(matchRepository, times(2)).save(any(Match.class));
  }

  @Test
  void processCompletedGame_whenSeriesClinchedAndAdvancingToPositionOne_setsTeamOne() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    Match nextMatch = match(tournament, false);
    nextMatch.setTeamOne(null);
    nextMatch.setTeamTwo(null);
    match.setNextMatch(nextMatch);
    match.setNextMatchPosition(1);
    match.getRound().setBestOf(3);
    match.setTeamOneWins(1);

    Game completedGame =
        Game.builder()
            .id(UUID.randomUUID())
            .status(GameStatus.COMPLETED)
            .winner(match.getTeamOne().getPlayerOne())
            .tournamentMatchId(match.getId())
            .build();
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    tournamentMatchService.processCompletedGame(completedGame.getId());

    assertThat(nextMatch.getTeamOne()).isEqualTo(match.getTeamOne());
  }

  @Test
  void processCompletedGame_whenFinalMatchClinched_completesTournament() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().setBestOf(3);
    match.setTeamOneWins(1);

    Game completedGame =
        Game.builder()
            .id(UUID.randomUUID())
            .status(GameStatus.COMPLETED)
            .winner(match.getTeamOne().getPlayerOne())
            .tournamentMatchId(match.getId())
            .build();
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    tournamentMatchService.processCompletedGame(completedGame.getId());

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
    verify(tournamentRepository).save(tournament);
  }

  @Test
  void getMatch_whenFound_returnsMatch() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

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
    when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tournamentMatchService.getMatch(tournament.getId(), matchId))
        .isInstanceOf(MatchNotFoundException.class);
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
            .roundNumber(1)
            .bestOf(1)
            .scoringMode(ScoringMode.STANDARD)
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

  // --- onGameCompleted listener ---

  @Test
  void onGameCompleted_whenTournamentMatchIdPresent_triggersMatchProgression() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.setStatus(MatchStatus.IN_PROGRESS);

    Game completedGame =
        Game.builder()
            .id(UUID.randomUUID())
            .status(GameStatus.COMPLETED)
            .winner(match.getTeamOne().getPlayerOne())
            .tournamentMatchId(match.getId())
            .build();
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    tournamentMatchService.onGameCompleted(
        new GameCompletedEvent(completedGame.getId(), match.getId()));

    assertThat(match.getTeamOneWins()).isEqualTo(1);
    verify(matchRepository, atLeastOnce()).save(any(Match.class));
  }

  @Test
  void onGameCompleted_whenTournamentMatchIdIsNull_doesNothing() {
    tournamentMatchService.onGameCompleted(new GameCompletedEvent(UUID.randomUUID(), null));

    verifyNoInteractions(gameRepository, matchRepository, tournamentRepository);
  }

  private User user(String suffix) {
    return TestDataFactory.user(
        UUID.randomUUID(), suffix, suffix + "@example.com", "encoded", Role.USER);
  }
}
