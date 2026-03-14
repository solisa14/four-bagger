package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository interface for managing {@link Match} entities. */
@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

  /**
   * Returns all matches belonging to a given tournament ordered by round then match number. Used to
   * navigate bracket structure without eager-loading the full tournament entity graph.
   *
   * @param tournamentId the ID of the tournament
   * @return matches ordered by round number then match number
   */
  List<Match> findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(UUID tournamentId);
}
