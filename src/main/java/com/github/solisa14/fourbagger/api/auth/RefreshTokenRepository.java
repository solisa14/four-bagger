package com.github.solisa14.fourbagger.api.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

/** Repository for managing {@link RefreshToken} entities. */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  /**
   * Finds a refresh token by its hash.
   *
   * @param tokenHash the hash of the refresh token
   * @return an Optional containing the token if found
   */
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Finds a refresh token by its associated user ID.
   *
   * @param userId the UUID of the user
   * @return an Optional containing the token if found
   */
  Optional<RefreshToken> findByUserId(UUID userId);

  /**
   * Deletes a refresh token associated with a specific user ID.
   *
   * @param userId the UUID of the user
   */
  @Modifying
  void deleteByUserId(UUID userId);

  /**
   * Deletes a refresh token by its hash.
   *
   * @param tokenHash the hash of the token to delete
   */
  @Modifying
  void deleteByTokenHash(String tokenHash);

  /**
   * Deletes all refresh tokens that have expired before the given time.
   *
   * @param now the current time
   */
  @Modifying
  void deleteByExpiryDateLessThan(Instant now);
}
