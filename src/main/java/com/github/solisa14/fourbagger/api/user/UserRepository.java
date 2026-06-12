package com.github.solisa14.fourbagger.api.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for User entity database operations.
 *
 * <p>Provides standard CRUD operations through JpaRepository and custom queries for username-based
 * lookups used during authentication and registration.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  /**
   * Queries the database for a user with the given username.
   *
   * <p>Used for username uniqueness validation during registration and credential lookup during
   * authentication. Username comparisons are case-sensitive.
   *
   * @param username the exact username to search for
   * @return Optional containing the user if found, empty if no match exists
   */
  Optional<User> findUserByUsername(String username);
}
