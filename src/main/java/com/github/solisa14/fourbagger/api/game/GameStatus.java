package com.github.solisa14.fourbagger.api.game;

/** Represents the current status of a game. */
public enum GameStatus {
  /** The game has been created but has not yet started. */
  PENDING,

  /** The game is currently being played. */
  IN_PROGRESS,

  /** The game has concluded and a winner has been determined. */
  COMPLETED,

  /** The game was cancelled before it could be completed. */
  CANCELLED
}
