package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentGameResultRepository extends JpaRepository<TournamentGameResult, UUID> {

  List<TournamentGameResult> findByMatchIdOrderByGameNumberAsc(UUID matchId);

  Optional<TournamentGameResult> findByMatchIdAndGameNumber(UUID matchId, int gameNumber);

  @Modifying
  @Query("delete from TournamentGameResult result where result.match.id = :matchId")
  void deleteByMatchId(@Param("matchId") UUID matchId);

  @Modifying
  @Query(
      """
      delete from TournamentGameResult result
      where result.match.round.tournament.id = :tournamentId
      """)
  void deleteByTournamentId(@Param("tournamentId") UUID tournamentId);
}
