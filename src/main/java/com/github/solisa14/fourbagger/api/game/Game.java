package com.github.solisa14.fourbagger.api.game;

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
 * Entity representing a cornhole game, either standalone or as part of a tournament match. Tracks
 * participants, scoring rules, current score, and the sequence of frames played.
 */
@Entity
@Table(name = "games")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
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

  public static Game createPending(
      User playerOne,
      User playerOnePartner,
      User playerTwo,
      User playerTwoPartner,
      GameType gameType,
      GameScoringMode scoringMode,
      int targetScore,
      User createdBy,
      UUID tournamentMatchId) {
    return Game.builder()
        .playerOne(playerOne)
        .playerOnePartner(playerOnePartner)
        .playerTwo(playerTwo)
        .playerTwoPartner(playerTwoPartner)
        .gameType(gameType)
        .scoringMode(scoringMode)
        .targetScore(targetScore)
        .status(GameStatus.PENDING)
        .createdBy(createdBy)
        .tournamentMatchId(tournamentMatchId)
        .build();
  }

  public static Game restore(
      User playerOne, User playerTwo, GameStatus status, User createdBy) {
    return Game.builder()
        .playerOne(playerOne)
        .playerTwo(playerTwo)
        .status(status)
        .createdBy(createdBy)
        .build();
  }

  public List<Frame> getFrames() {
    return Collections.unmodifiableList(frames);
  }

  public void addFrame(Frame frame) {
    frame.assignGame(this);
    frames.add(frame);
  }

  public void replaceFrames(List<Frame> replacementFrames) {
    List<Frame> replacements = List.copyOf(replacementFrames);
    frames.forEach(frame -> frame.detachGame(this));
    frames.clear();
    replacements.forEach(this::addFrame);
  }

  public void start() {
    if (status != GameStatus.PENDING) {
      throw new InvalidGameStateException(
          "Cannot start a game that is not in PENDING status. Current status: " + status);
    }
    status = GameStatus.IN_PROGRESS;
  }

  public void cancel() {
    if (status == GameStatus.COMPLETED || status == GameStatus.CANCELLED) {
      throw new InvalidGameStateException("Cannot cancel a game with status: " + status);
    }
    status = GameStatus.CANCELLED;
  }

  void addScores(int playerOnePoints, int playerTwoPoints) {
    playerOneScore += playerOnePoints;
    playerTwoScore += playerTwoPoints;
  }

  void resetPlayerOneScore(int score) {
    playerOneScore = score;
  }

  void resetPlayerTwoScore(int score) {
    playerTwoScore = score;
  }

  void complete(User winner) {
    if (winner != playerOne && winner != playerTwo) {
      throw new InvalidGameStateException("Winner must be a primary game participant");
    }
    this.winner = winner;
    status = GameStatus.COMPLETED;
  }
}
