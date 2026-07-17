package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Centralized policy for tournament bracket participant eligibility. */
@Component
public class TournamentBracketEligibilityPolicy {

  /**
   * Evaluates whether the tournament has enough participants to generate a bracket.
   *
   * @param tournament the tournament to evaluate
   * @return eligibility details including counts, requirements, and a user-facing message
   */
  public BracketEligibility evaluate(Tournament tournament) {
    int participantCount = tournament.getParticipants().size();
    int minimum = minimumParticipantCount(tournament.getGameType(), tournament.getFormat());
    boolean requiresEven = requiresEvenParticipantCount(tournament.getGameType());
    boolean eligible = isEligible(participantCount, minimum, requiresEven);
    String message = eligibilityMessage(participantCount, minimum, requiresEven, eligible);

    if (eligible && tournament.isManualDoubles() && !hasCompleteManualPairings(tournament)) {
      eligible = false;
      message = "Every guest must belong to exactly one complete team.";
    }

    return new BracketEligibility(
        eligible, participantCount, minimum, requiresEven, message);
  }

  /**
   * Validates bracket eligibility and throws if requirements are not met.
   *
   * @param tournament the tournament to validate
   * @throws InvalidTournamentStateException when participant requirements are not satisfied
   */
  public void validateForBracketGeneration(Tournament tournament) {
    BracketEligibility eligibility = evaluate(tournament);
    if (!eligibility.eligible()) {
      throw new InvalidTournamentStateException(eligibility.message());
    }
  }

  private int minimumParticipantCount(GameType gameType, TournamentFormat format) {
    if (gameType == GameType.SINGLES) {
      return format == TournamentFormat.SINGLE_ELIMINATION ? 3 : 4;
    }
    return format == TournamentFormat.SINGLE_ELIMINATION ? 6 : 8;
  }

  private boolean requiresEvenParticipantCount(GameType gameType) {
    return gameType == GameType.DOUBLES;
  }

  private boolean isEligible(int participantCount, int minimum, boolean requiresEven) {
    if (participantCount < minimum) {
      return false;
    }
    return !requiresEven || participantCount % 2 == 0;
  }

  private String eligibilityMessage(
      int participantCount, int minimum, boolean requiresEven, boolean eligible) {
    if (eligible) {
      return "Participant requirements are met.";
    }
    if (participantCount < minimum) {
      return "At least " + minimum + " participants are required.";
    }
    if (requiresEven && participantCount % 2 != 0) {
      return "Doubles tournaments require an even number of participants.";
    }
    return "Participant requirements are met.";
  }

  /**
   * Manual doubles requires every participant to appear on exactly one complete two-player team.
   */
  static boolean hasCompleteManualPairings(Tournament tournament) {
    if (tournament.getTeams().isEmpty()) {
      return false;
    }
    if (tournament.getTeams().size() * 2 != tournament.getParticipants().size()) {
      return false;
    }

    Set<UUID> assigned = new HashSet<>();
    for (TournamentTeam team : tournament.getTeams()) {
      if (team.getPlayerOne() == null || team.getPlayerTwo() == null) {
        return false;
      }
      UUID playerOneId = team.getPlayerOne().getId();
      UUID playerTwoId = team.getPlayerTwo().getId();
      if (playerOneId == null || playerTwoId == null || playerOneId.equals(playerTwoId)) {
        return false;
      }
      if (!assigned.add(playerOneId) || !assigned.add(playerTwoId)) {
        return false;
      }
    }

    return assigned.size() == tournament.getParticipants().size()
        && tournament.getParticipants().stream()
            .map(TournamentParticipant::getId)
            .allMatch(assigned::contains);
  }
}
