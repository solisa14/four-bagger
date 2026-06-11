package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
  @EntityGraph(attributePaths = {"round", "teamOne", "teamTwo", "winner"})
  List<Match> findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(UUID tournamentId);

  @EntityGraph(
      attributePaths = {
        "round",
        "round.tournament",
        "teamOne.playerOne",
        "teamOne.playerTwo",
        "teamTwo.playerOne",
        "teamTwo.playerTwo",
        "winner.playerOne",
        "winner.playerTwo",
        "winnerNextMatch",
        "winnerNextMatch.round",
        "winnerNextMatch.teamOne",
        "winnerNextMatch.teamTwo",
        "loserNextMatch",
        "loserNextMatch.round",
        "loserNextMatch.teamOne",
        "loserNextMatch.teamTwo"
      })
  @Query("select match from Match match where match.id = :matchId")
  Optional<Match> findForResponseById(@Param("matchId") UUID matchId);
}
