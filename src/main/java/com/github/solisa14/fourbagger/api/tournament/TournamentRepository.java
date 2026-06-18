package com.github.solisa14.fourbagger.api.tournament;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  List<Tournament> findByOrganizer_IdAndStatusInOrderByUpdatedAtDesc(
      UUID organizerId, Collection<TournamentStatus> statuses);

  @Query(
      """
      select distinct participant.tournament
      from TournamentParticipant participant
      where participant.user.id = :userId
        and participant.tournament.status in :statuses
      order by participant.tournament.updatedAt desc
      """)
  List<Tournament> findParticipatingActiveTournaments(
      @Param("userId") UUID userId, @Param("statuses") Collection<TournamentStatus> statuses);
}
