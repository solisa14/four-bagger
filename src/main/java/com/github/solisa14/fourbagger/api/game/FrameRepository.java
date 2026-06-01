package com.github.solisa14.fourbagger.api.game;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository interface for managing {@link Frame} entities. */
@Repository
public interface FrameRepository extends JpaRepository<Frame, UUID> {}
