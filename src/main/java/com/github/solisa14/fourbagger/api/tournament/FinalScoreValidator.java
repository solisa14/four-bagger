package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Shared validation for final-score submissions. */
@Component
public class FinalScoreValidator {

  public void validateScores(int sideOneScore, int sideTwoScore) {
    if (sideOneScore < 0 || sideTwoScore < 0) {
      throw new BusinessException("Scores must be nonnegative", HttpStatus.BAD_REQUEST);
    }
    if (sideOneScore == sideTwoScore) {
      throw new BusinessException("Scores cannot be tied", HttpStatus.BAD_REQUEST);
    }
  }

  public void validateWinnerHasHigherScore(
      UUID winnerTeamId, UUID teamOneId, UUID teamTwoId, int teamOneScore, int teamTwoScore) {
    if (winnerTeamId.equals(teamOneId) && teamOneScore <= teamTwoScore) {
      throw new BusinessException(
          "Winner team must have the higher score", HttpStatus.BAD_REQUEST);
    }
    if (winnerTeamId.equals(teamTwoId) && teamTwoScore <= teamOneScore) {
      throw new BusinessException(
          "Winner team must have the higher score", HttpStatus.BAD_REQUEST);
    }
    if (!winnerTeamId.equals(teamOneId) && !winnerTeamId.equals(teamTwoId)) {
      throw new BusinessException("Winner team is not a match participant", HttpStatus.BAD_REQUEST);
    }
  }
}
