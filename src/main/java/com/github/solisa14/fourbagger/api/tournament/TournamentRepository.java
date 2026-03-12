package com.github.solisa14.fourbagger.api.tournament;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository interface for managing {@link Tournament} entities. */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

  /**
   * Retrieves a tournament by its unique join code.
   *
   * @param joinCode the join code used to find the tournament
   * @return an {@link Optional} containing the tournament if found, or empty otherwise
   */
  Optional<Tournament> findByJoinCode(String joinCode);
}
