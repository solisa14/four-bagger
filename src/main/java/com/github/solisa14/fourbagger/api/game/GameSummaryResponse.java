package com.github.solisa14.fourbagger.api.game;

import java.time.Instant;
import java.util.UUID;

/** Summary view of a game for list endpoints. */
public record GameSummaryResponse(
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
    Instant completedAt,
    Instant createdAt,
    Instant updatedAt) {}
