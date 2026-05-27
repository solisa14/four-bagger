package com.github.solisa14.fourbagger.api.game;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.github.solisa14.fourbagger.api.user.User;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a cornhole game, either standalone or as part of a tournament match. Tracks
 * participants, scoring rules, current score, and the sequence of frames played.
 */
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

  @Enumerated(EnumType.STRING)
  @Column(name = "scoring_mode", nullable = false, length = 50)
  @Builder.Default
  private GameScoringMode scoringMode = GameScoringMode.STANDARD;

  @Column(name = "player_one_score", nullable = false)
  @Builder.Default
  private int playerOneScore = 0;

  @Column(name = "player_two_score", nullable = false)
  @Builder.Default
  private int playerTwoScore = 0;

  @Column(name = "target_score", nullable = false)
  @Builder.Default
  private int targetScore = 21;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private GameStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "winner_id")
  private User winner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @Column(name = "tournament_match_id")
  private UUID tournamentMatchId;

  @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("frameNumber ASC")
  @Builder.Default
  private List<Frame> frames = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;

}
