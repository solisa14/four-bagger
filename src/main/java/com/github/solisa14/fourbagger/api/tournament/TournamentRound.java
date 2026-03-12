package com.github.solisa14.fourbagger.api.tournament;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Represents a specific round within a tournament bracket (e.g., Quarterfinals, Semifinals). It
 * dictates the scoring rules and the number of games required to win a match in this round.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(
    name = "tournament_rounds",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_tournament_rounds_tournament_round_number",
          columnNames = {"tournament_id", "round_number"})
    })
public class TournamentRound {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

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
}
