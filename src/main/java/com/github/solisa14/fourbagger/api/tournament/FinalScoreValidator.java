package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
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
}
