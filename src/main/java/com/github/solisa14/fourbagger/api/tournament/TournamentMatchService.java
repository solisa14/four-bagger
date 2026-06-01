package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.CreateGameCommand;
import com.github.solisa14.fourbagger.api.game.Game;
import com.github.solisa14.fourbagger.api.game.GameCreationService;
import com.github.solisa14.fourbagger.api.game.GameRepository;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for controller-facing tournament match actions. Starts matches by creating
 * backing games and reads match state, while completed-game tournament advancement lives in {@link
 * TournamentProgressionService}.
 */
@Service
public class TournamentMatchService {

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;
  private final GameRepository gameRepository;
  private final GameCreationService gameCreationService;
  private final TournamentGameCommandFactory tournamentGameCommandFactory;

  /**
   * Constructs a new TournamentMatchService with required dependencies.
   *
   * @param tournamentRepository the repository for tournament data access
   * @param matchRepository the repository for match data access
   * @param gameRepository the repository for game data access
   * @param gameCreationService the service for handling complex game creation logic
   * @param tournamentGameCommandFactory the factory for converting tournament matches into game
   *     creation commands
   */
  public TournamentMatchService(
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
   * Starts a match by creating its first game (or retrieving an existing uncompleted game).
   * Validates that both teams are assigned and the match is ready to be played.
   *
   * @param tournamentId the UUID of the tournament
   * @param matchId the UUID of the match to start
   * @return the Game instance created or retrieved for this match
   * @throws InvalidTournamentStateException if the tournament or match is not ready to start
   * @throws MatchNotFoundException if the match cannot be found
   */
  @Transactional
  public Game startMatch(UUID tournamentId, UUID matchId, User currentUser) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    if (!tournament.getOrganizer().getId().equals(currentUser.getId())) {
      throw new TournamentAccessDeniedException(tournamentId);
    }
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
      return existingGames.getLast();
    }

    CreateGameCommand command =
        tournamentGameCommandFactory.createCommand(match, tournament.getOrganizer());

    Game game = gameCreationService.createPendingGame(command);
    match.setStatus(MatchStatus.IN_PROGRESS);
    matchRepository.save(match);
    return game;
  }

  /**
   * Retrieves a match by its ID, validating that it belongs to the specified tournament.
   *
   * @param tournamentId the UUID of the tournament
   * @param matchId the UUID of the match to retrieve
   * @return the Match entity
   * @throws TournamentNotFoundException if the tournament cannot be found
   * @throws MatchNotFoundException if the match cannot be found
   * @throws InvalidTournamentStateException if the match does not belong to the tournament
   */
  @Transactional(readOnly = true)
  public Match getMatch(UUID tournamentId, UUID matchId) {
    tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    Match match =
        matchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    validateMatchBelongsToTournament(tournamentId, match);
    return match;
  }

  private void validateMatchBelongsToTournament(UUID tournamentId, Match match) {
    UUID ownerTournamentId = match.getRound().getTournament().getId();
    if (!tournamentId.equals(ownerTournamentId)) {
      throw new InvalidTournamentStateException("Match does not belong to this tournament");
    }
  }
}
