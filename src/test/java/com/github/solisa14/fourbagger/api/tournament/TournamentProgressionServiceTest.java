package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.solisa14.fourbagger.api.game.CreateGameCommand;
import com.github.solisa14.fourbagger.api.game.Game;
import com.github.solisa14.fourbagger.api.game.GameCreationService;
import com.github.solisa14.fourbagger.api.game.GameRepository;
import com.github.solisa14.fourbagger.api.game.GameScoringMode;
import com.github.solisa14.fourbagger.api.game.GameStatus;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentProgressionServiceTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private MatchRepository matchRepository;
  @Mock private GameRepository gameRepository;
  @Mock private GameCreationService gameCreationService;
  @Spy private TournamentGameCommandFactory tournamentGameCommandFactory;

  @InjectMocks private TournamentProgressionService tournamentProgressionService;

  @Test
  void processCompletedGame_whenSeriesNotDecided_incrementsWinsAndCreatesNextGame() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().updateSettings(3, null);
    match.start();

    Game completedGame = completedGame(match);
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenReturn(Game.builder().id(UUID.randomUUID()).build());

    tournamentProgressionService.processCompletedGame(completedGame.getId());

    assertThat(match.getTeamOneWins()).isEqualTo(1);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    verify(gameCreationService).createPendingGame(any(CreateGameCommand.class));
    verify(matchRepository).save(match);
  }

  @Test
  void processCompletedGame_whenBestOfFive_requiresThreeWinsToClinch() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().updateSettings(5, null);
    match.start();
    match.recordWin(match.getTeamOne());

    Game completedGame = completedGame(match);
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenReturn(Game.builder().id(UUID.randomUUID()).build());

    tournamentProgressionService.processCompletedGame(completedGame.getId());

    assertThat(match.getTeamOneWins()).isEqualTo(2);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    verify(gameCreationService).createPendingGame(any(CreateGameCommand.class));
  }

  @Test
  void processCompletedGame_whenRoundUsesExactScoring_nextGameUsesExactScoringMode() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().updateSettings(3, ScoringMode.EXACT);
    match.start();

    Game completedGame = completedGame(match);
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(gameCreationService.createPendingGame(any(CreateGameCommand.class)))
        .thenReturn(Game.builder().id(UUID.randomUUID()).build());

    tournamentProgressionService.processCompletedGame(completedGame.getId());

    ArgumentCaptor<CreateGameCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateGameCommand.class);
    verify(gameCreationService).createPendingGame(commandCaptor.capture());
    assertThat(commandCaptor.getValue().resolvedScoringMode()).isEqualTo(GameScoringMode.EXACT);
  }

  @Test
  void processCompletedGame_whenSeriesClinched_advancesWinnerToNextMatch() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    Match winnerNextMatch = match(tournament, false);
    winnerNextMatch.assignTeams(null, null);
    match.configureWinnerRoute(winnerNextMatch, 2);
    match.getRound().updateSettings(3, null);
    match.recordWin(match.getTeamOne());

    Game completedGame = completedGame(match);
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    tournamentProgressionService.processCompletedGame(completedGame.getId());

    assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    assertThat(match.getWinner()).isEqualTo(match.getTeamOne());
    assertThat(winnerNextMatch.getTeamTwo()).isEqualTo(match.getTeamOne());
    verify(matchRepository, times(2)).save(any(Match.class));
  }

  @Test
  void processCompletedGame_whenSeriesClinchedAndAdvancingToPositionOne_setsTeamOne() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    Match winnerNextMatch = match(tournament, false);
    winnerNextMatch.assignTeams(null, null);
    match.configureWinnerRoute(winnerNextMatch, 1);
    match.getRound().updateSettings(3, null);
    match.recordWin(match.getTeamOne());

    Game completedGame = completedGame(match);
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    tournamentProgressionService.processCompletedGame(completedGame.getId());

    assertThat(winnerNextMatch.getTeamOne()).isEqualTo(match.getTeamOne());
  }

  @Test
  void processCompletedGame_whenFinalMatchClinched_completesTournament() {
    Tournament tournament = tournament(TournamentStatus.IN_PROGRESS);
    Match match = match(tournament, false);
    match.getRound().updateSettings(3, null);
    match.recordWin(match.getTeamOne());

    Game completedGame = completedGame(match);
    when(gameRepository.findById(completedGame.getId())).thenReturn(Optional.of(completedGame));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    tournamentProgressionService.processCompletedGame(completedGame.getId());

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
    verify(tournamentRepository).save(tournament);
  }

  private Game completedGame(Match match) {
    return Game.builder()
        .id(UUID.randomUUID())
        .status(GameStatus.COMPLETED)
        .winner(match.getTeamOne().getPlayerOne())
        .tournamentMatchId(match.getId())
        .build();
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

  private User user(String suffix) {
    return TestDataFactory.user(
        UUID.randomUUID(), suffix, suffix + "@example.com", "encoded", Role.USER);
  }
}
