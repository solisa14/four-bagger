package com.github.solisa14.fourbagger.api.tournament;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Represents a single contest between two teams within a tournament round. A match may consist of
 * one or more games depending on the round's "best of" configuration. It tracks the wins for each
 * team and routes the winner and loser to their configured next matches in the bracket graph.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(access = AccessLevel.PACKAGE)
@Entity
@Table(
    name = "tournament_matches",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_tournament_matches_round_match_number",
          columnNames = {"round_id", "match_number"})
    })
public class Match {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "round_id", nullable = false)
  private TournamentRound round;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_one_id")
  private TournamentTeam teamOne;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_two_id")
  private TournamentTeam teamTwo;

  @Column(name = "match_number", nullable = false)
  private Integer matchNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "winner_next_match_id")
  private Match winnerNextMatch;

  @Column(name = "winner_next_match_position")
  private Integer winnerNextMatchPosition;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "loser_next_match_id")
  private Match loserNextMatch;

  @Column(name = "loser_next_match_position")
  private Integer loserNextMatchPosition;

  @Column(name = "is_bye", nullable = false)
  @Builder.Default
  private boolean isBye = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private MatchStatus status = MatchStatus.PENDING;

  @Column(name = "team_one_wins", nullable = false)
  @Builder.Default
  private Integer teamOneWins = 0;

  @Column(name = "team_two_wins", nullable = false)
  @Builder.Default
  private Integer teamTwoWins = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "winner_id")
  private TournamentTeam winner;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  static Match create(TournamentRound round, int matchNumber) {
    return Match.builder()
        .round(round)
        .matchNumber(matchNumber)
        .status(MatchStatus.PENDING)
        .build();
  }

  void assignRound(TournamentRound round) {
    if (this.round != null && this.round != round) {
      throw new IllegalArgumentException("Match already belongs to another round");
    }
    this.round = round;
  }

  void detachRound(TournamentRound round) {
    if (this.round == round) {
      this.round = null;
    }
  }

  public void configureWinnerRoute(Match nextMatch, Integer position) {
    validateRoute(nextMatch, position);
    winnerNextMatch = nextMatch;
    winnerNextMatchPosition = position;
  }

  public void configureLoserRoute(Match nextMatch, Integer position) {
    validateRoute(nextMatch, position);
    loserNextMatch = nextMatch;
    loserNextMatchPosition = position;
  }

  private void validateRoute(Match nextMatch, Integer position) {
    if ((nextMatch == null) != (position == null)) {
      throw new IllegalArgumentException("Next match and position must both be set or both be null");
    }
    if (position != null && position != 1 && position != 2) {
      throw new IllegalArgumentException("Next match position must be 1 or 2");
    }
  }

  public void assignTeams(TournamentTeam teamOne, TournamentTeam teamTwo) {
    this.teamOne = teamOne;
    this.teamTwo = teamTwo;
  }

  public void assignTeam(int position, TournamentTeam team) {
    if (position == 1) {
      teamOne = team;
    } else if (position == 2) {
      teamTwo = team;
    } else {
      throw new IllegalArgumentException("Team position must be 1 or 2");
    }
  }

  public void resetForSeeding(TournamentTeam teamOne, TournamentTeam teamTwo) {
    this.teamOne = teamOne;
    this.teamTwo = teamTwo;
    teamOneWins = 0;
    teamTwoWins = 0;
    winner = null;
    isBye = false;
    status = MatchStatus.PENDING;
  }

  public void markBye(TournamentTeam advancingTeam) {
    assignTeams(advancingTeam, null);
    isBye = true;
    winner = advancingTeam;
    status = MatchStatus.COMPLETED;
  }

  public void start() {
    if (status == MatchStatus.COMPLETED || isBye) {
      throw new InvalidTournamentStateException("Completed or bye matches cannot be started");
    }
    status = MatchStatus.IN_PROGRESS;
  }

  public void recordWin(TournamentTeam winningTeam) {
    if (winningTeam == teamOne) {
      teamOneWins++;
    } else if (winningTeam == teamTwo) {
      teamTwoWins++;
    } else {
      throw new InvalidTournamentStateException("Winning team is not assigned to this match");
    }
  }

  public void complete(TournamentTeam winningTeam) {
    if (winningTeam != teamOne && winningTeam != teamTwo) {
      throw new InvalidTournamentStateException("Winning team is not assigned to this match");
    }
    winner = winningTeam;
    status = MatchStatus.COMPLETED;
  }
}
