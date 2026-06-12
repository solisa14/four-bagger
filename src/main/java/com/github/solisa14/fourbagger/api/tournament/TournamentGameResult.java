package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Final score result for one physical game within a tournament match series. */
@Entity
@Table(
    name = "tournament_game_results",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_tournament_game_results_match_game_number",
          columnNames = {"match_id", "game_number"})
    })
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class TournamentGameResult {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  private Match match;

  @Column(name = "game_number", nullable = false)
  private int gameNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "winner_team_id", nullable = false)
  private TournamentTeam winnerTeam;

  @Column(name = "team_one_score", nullable = false)
  private int teamOneScore;

  @Column(name = "team_two_score", nullable = false)
  private int teamTwoScore;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "submitted_by_id", nullable = false)
  private User submittedBy;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
