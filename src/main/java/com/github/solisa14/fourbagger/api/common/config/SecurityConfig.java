package com.github.solisa14.fourbagger.api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Provides Spring beans for application security components.
 *
 * <p>Configures password hashing and other security-related utilities used throughout the
 * application for credential management.
 */
@Component
public class SecurityConfig {

  /**
   * Provides a BCrypt password encoder for secure password hashing.
   *
   * <p>Uses BCrypt's adaptive hashing algorithm with default strength (10 rounds) to protect user
   * passwords in the database.
   *
   * @return configured BCrypt encoder instance
   */
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
