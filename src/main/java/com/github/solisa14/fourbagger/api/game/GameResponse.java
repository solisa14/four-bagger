package com.github.solisa14.fourbagger.api.game;

import java.time.Instant;
import java.util.UUID;

/** Data Transfer Object representing the full details of a standalone game. */
public record GameResponse(
    UUID id,
    GameType gameType,
    PlayerInfo playerOne,
    PlayerInfo playerOnePartner,
    PlayerInfo playerTwo,
    PlayerInfo playerTwoPartner,
    int playerOneScore,
    int playerTwoScore,
    GameStatus status,
    PlayerInfo winner,
    PlayerInfo submittedBy,
    Instant completedAt,
    Instant createdAt,
    Instant updatedAt) {}
