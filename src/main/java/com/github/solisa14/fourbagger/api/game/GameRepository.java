package com.github.solisa14.fourbagger.api.game;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

  @Query(
      "SELECT g FROM Game g WHERE g.playerOne = :user OR g.playerTwo = :user ORDER BY g.createdAt DESC")
  List<Game> findByPlayer(User user);
}
