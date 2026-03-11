package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.CreateGameCommand;
import com.github.solisa14.fourbagger.api.game.Game;
import com.github.solisa14.fourbagger.api.game.GameCreationService;
import com.github.solisa14.fourbagger.api.game.GameParticipants;
import com.github.solisa14.fourbagger.api.game.GameRepository;
import com.github.solisa14.fourbagger.api.game.GameStatus;
import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.game.InvalidGameStateException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentMatchService {

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;
  private final GameRepository gameRepository;
  private final GameCreationService gameCreationService;

  public TournamentMatchService(
      TournamentRepository tournamentRepository,
      MatchRepository matchRepository,
      GameRepository gameRepository,
      GameCreationService gameCreationService) {
    this.tournamentRepository = tournamentRepository;
    this.matchRepository = matchRepository;
    this.gameRepository = gameRepository;
    this.gameCreationService = gameCreationService;
  }

  @Transactional
  public Game startMatch(UUID tournamentId, UUID matchId) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
      throw new InvalidTournamentStateException(
          "Cannot start a match unless the tournament is IN_PROGRESS");
    }

    Match match =
        matchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    validateMatchBelongsToTournament(tournamentId, match);
    if (match.isBye()) {
      throw new InvalidTournamentStateException("Cannot start a bye match");
    }
    if (match.getStatus() == MatchStatus.COMPLETED) {
      throw new InvalidTournamentStateException("Cannot start a completed match");
    }
    if (match.getTeamOne() == null || match.getTeamTwo() == null) {
      throw new InvalidTournamentStateException(
          "Cannot start a match until both teams are assigned");
    }

    List<Game> existingGames =
        gameRepository.findByTournamentMatchIdOrderByCreatedAtAsc(match.getId());
    if (!existingGames.isEmpty()) {
      if (match.getStatus() == MatchStatus.PENDING) {
        match.setStatus(MatchStatus.IN_PROGRESS);
        matchRepository.save(match);
      }
      return existingGames.getFirst();
    }

    GameParticipants participants = resolveParticipants(match);
    CreateGameCommand command =
        new CreateGameCommand(
            participants,
            resolveTargetScore(match.getRound().getScoringMode()),
            resolveWinByTwo(match.getRound().getScoringMode()),
            match.getId(),
            tournament.getOrganizer());

    Game game = gameCreationService.createPendingGame(command);
    match.setStatus(MatchStatus.IN_PROGRESS);
    matchRepository.save(match);
    return game;
  }

  @Transactional
  public void processCompletedGame(UUID gameId) {
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
    if (winningTeam.getId().equals(match.getTeamOne().getId())) {
      match.setTeamOneWins(match.getTeamOneWins() + 1);
    } else if (match.getTeamTwo() != null
        && winningTeam.getId().equals(match.getTeamTwo().getId())) {
      match.setTeamTwoWins(match.getTeamTwoWins() + 1);
    }

    int winsToClinch = (match.getRound().getBestOf() / 2) + 1;
    if (match.getTeamOneWins() >= winsToClinch || match.getTeamTwoWins() >= winsToClinch) {
      completeMatch(match, winningTeam);
      return;
    }

    CreateGameCommand command =
        new CreateGameCommand(
            resolveParticipants(match),
            resolveTargetScore(match.getRound().getScoringMode()),
            resolveWinByTwo(match.getRound().getScoringMode()),
            match.getId(),
            match.getRound().getTournament().getOrganizer());
    gameCreationService.createPendingGame(command);
    match.setStatus(MatchStatus.IN_PROGRESS);
    matchRepository.save(match);
  }

  private void completeMatch(Match match, TournamentTeam winningTeam) {
    match.setWinner(winningTeam);
    match.setStatus(MatchStatus.COMPLETED);
    matchRepository.save(match);

    Match nextMatch = match.getNextMatch();
    if (nextMatch != null) {
      if (match.getNextMatchPosition() != null && match.getNextMatchPosition() == 1) {
        nextMatch.setTeamOne(winningTeam);
      } else if (match.getNextMatchPosition() != null && match.getNextMatchPosition() == 2) {
        nextMatch.setTeamTwo(winningTeam);
      }
      matchRepository.save(nextMatch);
      return;
    }

    Tournament tournament = match.getRound().getTournament();
    tournament.setStatus(TournamentStatus.COMPLETED);
    tournamentRepository.save(tournament);
  }

  private void validateMatchBelongsToTournament(UUID tournamentId, Match match) {
    UUID ownerTournamentId = match.getRound().getTournament().getId();
    if (!tournamentId.equals(ownerTournamentId)) {
      throw new InvalidTournamentStateException("Match does not belong to this tournament");
    }
  }

  private GameParticipants resolveParticipants(Match match) {
    TournamentTeam teamOne = match.getTeamOne();
    TournamentTeam teamTwo = match.getTeamTwo();
    if (teamOne == null || teamTwo == null) {
      throw new InvalidTournamentStateException("Match participants are incomplete");
    }

    GameType gameType = resolveGameType(teamOne, teamTwo);
    if (gameType == GameType.DOUBLES) {
      return GameParticipants.doubles(
          teamOne.getPlayerOne(),
          teamOne.getPlayerTwo(),
          teamTwo.getPlayerOne(),
          teamTwo.getPlayerTwo());
    }
    return GameParticipants.singles(teamOne.getPlayerOne(), teamTwo.getPlayerOne());
  }

  private GameType resolveGameType(TournamentTeam teamOne, TournamentTeam teamTwo) {
    boolean teamOneIsDoubles = teamOne.getPlayerTwo() != null;
    boolean teamTwoIsDoubles = teamTwo.getPlayerTwo() != null;
    if (teamOneIsDoubles != teamTwoIsDoubles) {
      throw new InvalidTournamentStateException("Both teams must have the same game type");
    }
    return teamOneIsDoubles ? GameType.DOUBLES : GameType.SINGLES;
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

  private Integer resolveTargetScore(ScoringMode scoringMode) {
    return 21;
  }

  private Boolean resolveWinByTwo(ScoringMode scoringMode) {
    return false;
  }
}
