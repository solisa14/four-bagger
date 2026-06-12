package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentGameResultRepository extends JpaRepository<TournamentGameResult, UUID> {

  List<TournamentGameResult> findByMatchIdOrderByGameNumberAsc(UUID matchId);

  Optional<TournamentGameResult> findByMatchIdAndGameNumber(UUID matchId, int gameNumber);
}
