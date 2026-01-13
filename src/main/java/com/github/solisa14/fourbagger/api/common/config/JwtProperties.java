package com.github.solisa14.fourbagger.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for JWT (JSON Web Token) authentication settings.
 *
 * <p>This class binds configuration properties prefixed with "app.security.jwt" from
 * application configuration files (application.yml, application.properties, etc.).
 * It provides access to JWT-related settings such as the secret key and token expiration time.
 */
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

  /** The secret key used for signing and verifying JWT tokens. */
  private String secretKey;

  /** The token expiration time in milliseconds. Defaults to 3600000 (1 hour). */
  private long expirationMs = 3600000; // 1 hour

  /**
   * Returns the secret key used for JWT token signing and verification.
   *
   * @return the JWT secret key
   */
  public String getSecretKey() {
    return secretKey;
  }

  /**
   * Sets the secret key used for JWT token signing and verification.
   *
   * @param secretKey the JWT secret key to set
   */
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  /**
   * Returns the JWT token expiration time in milliseconds.
   *
   * @return the token expiration time in milliseconds
   */
  public long getExpirationMs() {
    return expirationMs;
  }

  /**
   * Sets the JWT token expiration time in milliseconds.
   *
   * @param expirationMs the token expiration time in milliseconds to set
   */
  public void setExpirationMs(long expirationMs) {
    this.expirationMs = expirationMs;
  }
}
