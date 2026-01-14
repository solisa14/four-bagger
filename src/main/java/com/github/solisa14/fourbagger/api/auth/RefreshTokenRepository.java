package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

/** Repository for managing {@link RefreshToken} entities. */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByToken(String token);

  @Modifying
  void deleteByUser(User user);

  @Modifying
  void deleteByToken(String token);

  @Modifying
  void deleteByExpiryDateLessThan(Instant now);
}
