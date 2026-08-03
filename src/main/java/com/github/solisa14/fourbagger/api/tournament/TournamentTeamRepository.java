package com.github.solisa14.fourbagger.api.tournament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, UUID> {}
