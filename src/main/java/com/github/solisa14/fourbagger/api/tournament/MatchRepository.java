package com.github.solisa14.fourbagger.api.tournament;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository interface for managing {@link Match} entities. */
@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {}
