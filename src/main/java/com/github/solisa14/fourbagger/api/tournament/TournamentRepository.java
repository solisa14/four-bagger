package com.github.solisa14.fourbagger.api.tournament;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

  Optional<Tournament> findByJoinCode(String joinCode);
}
