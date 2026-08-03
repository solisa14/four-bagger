package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Repository for standalone {@link Game} entities. */
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

    /** Games where the user is any side participant, newest first. */
    @Query("""
        SELECT g
        FROM Game g
        WHERE g.playerOne = :user
           OR g.playerTwo = :user
           OR g.playerOnePartner = :user
           OR g.playerTwoPartner = :user
        ORDER BY g.createdAt DESC
        """)
    List<Game> findByPlayer(User user);
}
