package com.github.solisa14.fourbagger.api.tournament;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
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
  @JoinColumn(name = "next_match_id")
  private Match nextMatch;

  @Column(name = "next_match_position")
  private Integer nextMatchPosition;

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
}
