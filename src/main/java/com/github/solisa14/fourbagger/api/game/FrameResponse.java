package com.github.solisa14.fourbagger.api.game;

import java.time.Instant;
import java.util.UUID;

public record FrameResponse(
    UUID id,
    int frameNumber,
    int playerOneBagsIn,
    int playerOneBagsOn,
    int playerTwoBagsIn,
    int playerTwoBagsOn,
    int playerOneFramePoints,
    int playerTwoFramePoints,
    Instant createdAt) {

  public static FrameResponse from(Frame frame) {
    return new FrameResponse(
        frame.getId(),
        frame.getFrameNumber(),
        frame.getPlayerOneBagsIn(),
        frame.getPlayerOneBagsOn(),
        frame.getPlayerTwoBagsIn(),
        frame.getPlayerTwoBagsOn(),
        frame.getPlayerOneFramePoints(),
        frame.getPlayerTwoFramePoints(),
        frame.getCreatedAt());
  }
}
