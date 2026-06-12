package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
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

/** Entity representing a standalone cornhole game with final-result scoring. */
@Entity
@Table(name = "games")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Game {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_one_id", nullable = false)
  private User playerOne;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_two_id", nullable = false)
  private User playerTwo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_one_partner_id")
  private User playerOnePartner;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_two_partner_id")
  private User playerTwoPartner;

  @Enumerated(EnumType.STRING)
  @Column(name = "game_type", nullable = false, length = 50)
  @Builder.Default
  private GameType gameType = GameType.SINGLES;

  @Column(name = "player_one_score", nullable = false)
  @Builder.Default
  private int playerOneScore = 0;

  @Column(name = "player_two_score", nullable = false)
  @Builder.Default
  private int playerTwoScore = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private GameStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "winner_id")
  private User winner;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by_id")
  private User submittedBy;

  @Column(name = "completed_at")
  private Instant completedAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @Column(name = "tournament_match_id")
  private UUID tournamentMatchId;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}
