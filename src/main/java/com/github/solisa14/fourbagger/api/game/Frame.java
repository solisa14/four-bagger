package com.github.solisa14.fourbagger.api.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Entity representing a single frame (or round) within a cornhole game. It tracks the number of
 * bags in the hole and on the board for each side, as well as the net points calculated for that
 * frame.
 */
@Entity
@Table(name = "frames")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Frame {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "game_id", nullable = false)
  private Game game;

  @Column(name = "frame_number", nullable = false)
  private int frameNumber;

  @Column(name = "player_one_bags_in", nullable = false)
  @Builder.Default
  private int playerOneBagsIn = 0;

  @Column(name = "player_one_bags_on", nullable = false)
  @Builder.Default
  private int playerOneBagsOn = 0;

  @Column(name = "player_two_bags_in", nullable = false)
  @Builder.Default
  private int playerTwoBagsIn = 0;

  @Column(name = "player_two_bags_on", nullable = false)
  @Builder.Default
  private int playerTwoBagsOn = 0;

  @Column(name = "player_one_frame_points", nullable = false)
  @Builder.Default
  private int playerOneFramePoints = 0;

  @Column(name = "player_two_frame_points", nullable = false)
  @Builder.Default
  private int playerTwoFramePoints = 0;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;
}
