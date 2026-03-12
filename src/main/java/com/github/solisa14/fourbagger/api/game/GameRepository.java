package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repository interface for managing {@link Game} entities. */
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

  /**
   * Finds all games where the specified user is either player one or player two. Order is by
   * creation time descending.
   *
   * @param user The user.
   * @return List of matching games.
   */
  @Query(
      "SELECT g FROM Game g WHERE g.playerOne = :user OR g.playerTwo = :user ORDER BY g.createdAt DESC")
  List<Game> findByPlayer(User user);

  /**
   * Finds games associated with a specific tournament match.
   *
   * @param tournamentMatchId The ID of the tournament match.
   * @return List of matching games ordered by creation time.
   */
  List<Game> findByTournamentMatchIdOrderByCreatedAtAsc(UUID tournamentMatchId);
}
