package com.github.solisa14.fourbagger.api.tournament;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Represents a specific round within a tournament bracket (e.g., Quarterfinals, Semifinals). It
 * dictates the scoring rules and the number of games required to win a match in this round.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(access = AccessLevel.PACKAGE)
@Entity
@Table(
    name = "tournament_rounds",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_tournament_rounds_tournament_bracket_round_number",
          columnNames = {"tournament_id", "bracket_type", "round_number"})
    })
public class TournamentRound {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

  @Column(name = "bracket_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private BracketType bracketType;

  @Column(name = "round_number", nullable = false)
  private Integer roundNumber;

  @Column(name = "best_of", nullable = false)
  @Builder.Default
  private Integer bestOf = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "scoring_mode", nullable = false)
  @Builder.Default
  private ScoringMode scoringMode = ScoringMode.STANDARD;

  @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("matchNumber ASC")
  @Builder.Default
  private List<Match> matches = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  static TournamentRound create(
      Tournament tournament,
      BracketType bracketType,
      int roundNumber,
      int bestOf,
      ScoringMode scoringMode) {
    return TournamentRound.builder()
        .tournament(tournament)
        .bracketType(bracketType)
        .roundNumber(roundNumber)
        .bestOf(bestOf)
        .scoringMode(scoringMode)
        .build();
  }

  public List<Match> getMatches() {
    return Collections.unmodifiableList(matches);
  }

  void assignTournament(Tournament tournament) {
    if (this.tournament != null && this.tournament != tournament) {
      throw new IllegalArgumentException("Round already belongs to another tournament");
    }
    this.tournament = tournament;
  }

  void detachTournament(Tournament tournament) {
    if (this.tournament == tournament) {
      this.tournament = null;
    }
  }

  public void addMatch(Match match) {
    match.assignRound(this);
    matches.add(match);
  }

  public void replaceMatches(List<Match> replacementMatches) {
    List<Match> replacements = List.copyOf(replacementMatches);
    matches.forEach(match -> match.detachRound(this));
    matches.clear();
    replacements.forEach(this::addMatch);
  }

  public void clearMatches() {
    matches.forEach(match -> match.detachRound(this));
    matches.clear();
  }

  public void updateSettings(Integer bestOf, ScoringMode scoringMode) {
    if (bestOf == null && scoringMode == null) {
      throw new InvalidRoundConfigurationException("At least one round setting must be provided");
    }
    if (bestOf != null && bestOf != 1 && bestOf != 3 && bestOf != 5 && bestOf != 7) {
      throw new InvalidRoundConfigurationException("bestOf must be one of: 1, 3, 5, or 7");
    }
    if (bestOf != null) {
      this.bestOf = bestOf;
    }
    if (scoringMode != null) {
      this.scoringMode = scoringMode;
    }
  }
}
