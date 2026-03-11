package com.github.solisa14.fourbagger.api.game;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameCreationService {

  private final GameRepository gameRepository;

  public GameCreationService(GameRepository gameRepository) {
    this.gameRepository = gameRepository;
  }

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
            .targetScore(command.resolvedTargetScore())
            .winByTwo(command.resolvedWinByTwo())
            .status(GameStatus.PENDING)
            .createdBy(command.createdBy())
            .tournamentMatchId(command.tournamentMatchId())
            .build();

    return gameRepository.save(game);
  }
}
