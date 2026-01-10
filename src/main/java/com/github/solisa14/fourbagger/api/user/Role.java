package com.github.solisa14.fourbagger.api.user;

/**
 * Defines authorization roles for user access control.
 *
 * <p>Controls what actions users can perform in the system. Mapped to Spring Security authorities
 * with "ROLE_" prefix for role-based authorization checks.
 */
public enum Role {
  /** Standard user with basic access permissions. */
  USER,
  /** Administrative user with elevated privileges. */
  ADMIN
}
