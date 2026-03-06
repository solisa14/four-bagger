package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for managing {@link RefreshToken} entities. */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  Optional<RefreshToken> findByUserId(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select refreshToken from RefreshToken refreshToken where refreshToken.tokenHash = :tokenHash")
  Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

  @Modifying
  void deleteByUser(User user);

  @Modifying
  void deleteByUserId(UUID userId);

  @Modifying
  void deleteByTokenHash(String tokenHash);

  @Modifying
  void deleteByExpiryDateLessThan(Instant now);
}
