package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.CreateGameCommand;
import com.github.solisa14.fourbagger.api.game.Game;
import com.github.solisa14.fourbagger.api.game.GameCreationService;
import com.github.solisa14.fourbagger.api.game.GameRepository;
import com.github.solisa14.fourbagger.api.game.GameStatus;
import com.github.solisa14.fourbagger.api.game.InvalidGameStateException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Advances tournament state after a tournament-backed game completes. This service owns series
 * wins, bracket advancement, and final tournament completion.
 */
@Service
class TournamentProgressionService {

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;
  private final GameRepository gameRepository;
  private final GameCreationService gameCreationService;
  private final TournamentGameCommandFactory tournamentGameCommandFactory;

  TournamentProgressionService(
      TournamentRepository tournamentRepository,
      MatchRepository matchRepository,
      GameRepository gameRepository,
      GameCreationService gameCreationService,
      TournamentGameCommandFactory tournamentGameCommandFactory) {
    this.tournamentRepository = tournamentRepository;
    this.matchRepository = matchRepository;
    this.gameRepository = gameRepository;
    this.gameCreationService = gameCreationService;
    this.tournamentGameCommandFactory = tournamentGameCommandFactory;
  }

  /**
   * Processes a completed game and advances its linked tournament match or tournament bracket.
   *
   * @param gameId the UUID of the completed game
   * @throws InvalidGameStateException if the game is not completed or not linked to a match
   */
  @Transactional
  void processCompletedGame(UUID gameId) {
    Game game =
        gameRepository
            .findById(gameId)
            .orElseThrow(() -> new InvalidGameStateException("Game not found"));
    if (game.getTournamentMatchId() == null) {
      throw new InvalidGameStateException("Game is not linked to a tournament match");
    }

    if (game.getStatus() != GameStatus.COMPLETED || game.getWinner() == null) {
      throw new InvalidGameStateException(
          "Game must be completed before processing tournament progression");
    }

    Match match =
        matchRepository
            .findById(game.getTournamentMatchId())
            .orElseThrow(() -> new MatchNotFoundException(game.getTournamentMatchId()));
    if (match.getStatus() == MatchStatus.COMPLETED) {
      return;
    }

    TournamentTeam winningTeam = resolveWinningTeam(match, game);
    incrementWins(match, winningTeam);

    if (isSeriesClinched(match)) {
      completeMatch(match, winningTeam);
      return;
    }

    CreateGameCommand command =
        tournamentGameCommandFactory.createCommand(
            match, match.getRound().getTournament().getOrganizer());
    gameCreationService.createPendingGame(command);
    match.setStatus(MatchStatus.IN_PROGRESS);
    matchRepository.save(match);
  }

  private TournamentTeam resolveWinningTeam(Match match, Game game) {
    UUID winnerId = game.getWinner().getId();
    TournamentTeam teamOne = match.getTeamOne();
    TournamentTeam teamTwo = match.getTeamTwo();
    if (teamOne != null
        && (teamOne.getPlayerOne().getId().equals(winnerId)
            || (teamOne.getPlayerTwo() != null
                && teamOne.getPlayerTwo().getId().equals(winnerId)))) {
      return teamOne;
    }
    if (teamTwo != null
        && (teamTwo.getPlayerOne().getId().equals(winnerId)
            || (teamTwo.getPlayerTwo() != null
                && teamTwo.getPlayerTwo().getId().equals(winnerId)))) {
      return teamTwo;
    }
    throw new InvalidTournamentStateException(
        "Game winner is not a participant in the linked match");
  }

  private void incrementWins(Match match, TournamentTeam winningTeam) {
    if (winningTeam.getId().equals(match.getTeamOne().getId())) {
      match.setTeamOneWins(match.getTeamOneWins() + 1);
    } else if (match.getTeamTwo() != null
        && winningTeam.getId().equals(match.getTeamTwo().getId())) {
      match.setTeamTwoWins(match.getTeamTwoWins() + 1);
    }
  }

  private boolean isSeriesClinched(Match match) {
    int winsToClinch = (match.getRound().getBestOf() / 2) + 1;
    return match.getTeamOneWins() >= winsToClinch || match.getTeamTwoWins() >= winsToClinch;
  }

  private void completeMatch(Match match, TournamentTeam winningTeam) {
    match.setWinner(winningTeam);
    match.setStatus(MatchStatus.COMPLETED);
    matchRepository.save(match);

    Match nextMatch = match.getNextMatch();
    if (nextMatch != null) {
      advanceWinner(match, winningTeam, nextMatch);
      matchRepository.save(nextMatch);
      return;
    }

    Tournament tournament = match.getRound().getTournament();
    tournament.setStatus(TournamentStatus.COMPLETED);
    tournamentRepository.save(tournament);
  }

  private void advanceWinner(Match match, TournamentTeam winningTeam, Match nextMatch) {
    if (match.getNextMatchPosition() != null && match.getNextMatchPosition() == 1) {
      nextMatch.setTeamOne(winningTeam);
    } else if (match.getNextMatchPosition() != null && match.getNextMatchPosition() == 2) {
      nextMatch.setTeamTwo(winningTeam);
    }
  }
}
