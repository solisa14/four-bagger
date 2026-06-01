package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.CreateGameCommand;
import com.github.solisa14.fourbagger.api.game.GameParticipants;
import com.github.solisa14.fourbagger.api.game.GameScoringMode;
import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.user.User;
import org.springframework.stereotype.Component;

/** Creates game commands from tournament match state. */
@Component
class TournamentGameCommandFactory {

  CreateGameCommand createCommand(Match match, User createdBy) {
    return new CreateGameCommand(
        resolveParticipants(match),
        21,
        resolveGameScoringMode(match.getRound().getScoringMode()),
        match.getId(),
        createdBy);
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

  private GameScoringMode resolveGameScoringMode(ScoringMode scoringMode) {
    if (scoringMode == ScoringMode.EXACT) {
      return GameScoringMode.EXACT;
    }
    return GameScoringMode.STANDARD;
  }
}
