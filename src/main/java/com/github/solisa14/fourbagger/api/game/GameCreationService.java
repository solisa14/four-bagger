package com.github.solisa14.fourbagger.api.game;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service dedicated to handling the creation of new game instances. */
@Service
public class GameCreationService {

  private final GameRepository gameRepository;

  public GameCreationService(GameRepository gameRepository) {
    this.gameRepository = gameRepository;
  }

  /**
   * Creates a new pending game based on the provided command.
   *
   * @param command The command object containing game configuration and participants.
   * @return The newly created and saved game.
   */
  @Transactional
  public Game createPendingGame(CreateGameCommand command) {
    GameParticipants participants = command.participants();
    Game game =
        Game.createPending(
            participants.teamOne().player(),
            participants.teamOne().partner(),
            participants.teamTwo().player(),
            participants.teamTwo().partner(),
            participants.gameType(),
            command.resolvedScoringMode(),
            command.resolvedTargetScore(),
            command.createdBy(),
            command.tournamentMatchId());

    return gameRepository.save(game);
  }
}
