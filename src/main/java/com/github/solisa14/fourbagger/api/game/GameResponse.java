package com.github.solisa14.fourbagger.api.game;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing the full details of a game. Includes participant info, current
 * scores, status, and the list of frames.
 *
 * @param id The unique identifier of the game.
 * @param gameType The type of game (e.g., SINGLES, DOUBLES).
 * @param playerOne The primary player for team one.
 * @param playerOnePartner The partner for team one (if doubles).
 * @param playerTwo The primary player for team two.
 * @param playerTwoPartner The partner for team two (if doubles).
 * @param playerOneScore The current score for team one.
 * @param playerTwoScore The current score for team two.
 * @param targetScore The score required to win the game.
 * @param winByTwo Indicates if the game requires a two-point margin to win.
 * @param status The current status of the game.
 * @param winner The player or team that won the game.
 * @param frames The sequence of frames played in the game.
 * @param createdAt The timestamp when the game was created.
 * @param updatedAt The timestamp when the game was last updated.
 */
public record GameResponse(
    UUID id,
    GameType gameType,
    PlayerInfo playerOne,
    PlayerInfo playerOnePartner,
    PlayerInfo playerTwo,
    PlayerInfo playerTwoPartner,
    int playerOneScore,
    int playerTwoScore,
    int targetScore,
    boolean winByTwo,
    GameStatus status,
    PlayerInfo winner,
    List<FrameResponse> frames,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Factory method to create a {@link GameResponse} from a {@link Game} entity.
   *
   * @param game The game entity.
   * @return The game response object.
   */
  public static GameResponse from(Game game) {
    return new GameResponse(
        game.getId(),
        game.getGameType(),
        PlayerInfo.from(game.getPlayerOne()),
        game.getPlayerOnePartner() != null ? PlayerInfo.from(game.getPlayerOnePartner()) : null,
        PlayerInfo.from(game.getPlayerTwo()),
        game.getPlayerTwoPartner() != null ? PlayerInfo.from(game.getPlayerTwoPartner()) : null,
        game.getPlayerOneScore(),
        game.getPlayerTwoScore(),
        game.getTargetScore(),
        game.isWinByTwo(),
        game.getStatus(),
        game.getWinner() != null ? PlayerInfo.from(game.getWinner()) : null,
        game.getFrames().stream().map(FrameResponse::from).toList(),
        game.getCreatedAt(),
        game.getUpdatedAt());
  }
}
