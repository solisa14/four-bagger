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

  /**
   * Retrieves the tournament detail access graph without joining the separate bracket collections.
   *
   * <p>Participants and their users are fetched together because registration detail pages poll
   * while open. Keeping rounds out of this query avoids joining multiple list-valued associations.
   */
  @Query(
      """
      select distinct tournament
      from Tournament tournament
      join fetch tournament.organizer
      left join fetch tournament.participants participant
      left join fetch participant.user
      where tournament.id = :id
      """)
  Optional<Tournament> findDetailById(@Param("id") UUID id);

  /**
   * Retrieves the tournament detail access graph by join code.
   *
   * <p>Same fetch graph as {@link #findDetailById(UUID)} so invite previews can serialize
   * organizer and participant details outside the service transaction.
   */
  @Query(
      """
      select distinct tournament
      from Tournament tournament
      join fetch tournament.organizer
      left join fetch tournament.participants participant
      left join fetch participant.user
      where tournament.joinCode = :joinCode
      """)
  Optional<Tournament> findDetailByJoinCode(@Param("joinCode") String joinCode);

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

  @Query(
      """
      select distinct t
      from Tournament t
      left join t.participants p
      where t.status = 'COMPLETED'
        and (t.organizer.id = :userId or p.user.id = :userId)
      order by t.updatedAt desc
      """)
  List<Tournament> findCompletedTournamentsForUser(@Param("userId") UUID userId);
}
