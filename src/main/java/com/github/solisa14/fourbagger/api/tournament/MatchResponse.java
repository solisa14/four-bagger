package com.github.solisa14.fourbagger.api.tournament;

import java.util.UUID;

/**
 * Data Transfer Object representing the state of a tournament match. Includes team summaries, win
 * counts, and the current match status.
 *
 * @param id the unique identifier of the match
 * @param matchNumber the positional number of this match within its round
 * @param status the current status of the match
 * @param isBye whether this match is a bye (automatic advancement)
 * @param teamOne summary of the first team, or null if not yet assigned
 * @param teamTwo summary of the second team, or null if not yet assigned
 * @param teamOneWins the number of games won by team one in this match series
 * @param teamTwoWins the number of games won by team two in this match series
 * @param winner summary of the winning team, or null if the match is not yet completed
 */
public record MatchResponse(
    UUID id,
    int matchNumber,
    MatchStatus status,
    boolean isBye,
    TeamSummary teamOne,
    TeamSummary teamTwo,
    int teamOneWins,
    int teamTwoWins,
    TeamSummary winner) {

  /**
   * Summary of a tournament team for inclusion in match responses.
   *
   * @param id the team's unique identifier
   * @param playerOneUsername username of the first player
   * @param playerTwoUsername username of the second player, or null for singles
   * @param seed the team's seed number in the bracket
   */
  public record TeamSummary(
      UUID id, String playerOneUsername, String playerTwoUsername, Integer seed) {
  }
}
