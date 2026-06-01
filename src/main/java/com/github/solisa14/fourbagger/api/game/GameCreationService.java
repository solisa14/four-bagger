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
        Game.builder()
            .playerOne(participants.teamOne().player())
            .playerOnePartner(participants.teamOne().partner())
            .playerTwo(participants.teamTwo().player())
            .playerTwoPartner(participants.teamTwo().partner())
            .gameType(participants.gameType())
            .scoringMode(command.resolvedScoringMode())
            .targetScore(command.resolvedTargetScore())
            .status(GameStatus.PENDING)
            .createdBy(command.createdBy())
            .tournamentMatchId(command.tournamentMatchId())
            .build();

    return gameRepository.save(game);
  }
}
