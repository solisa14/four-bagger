package com.github.solisa14.fourbagger.api.config;

import java.util.Base64;
import java.util.Locale;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-closed startup checks for the {@code prod} profile.
 *
 * <p>Rejects missing required secrets, the known development JWT placeholder, under-sized signing
 * keys, and localhost CORS origins before the application serves traffic.
 */
@Component
@Profile("prod")
public class ProdSecurityStartupValidator implements InitializingBean {

  /** Base64-encoded 32-byte placeholder used by the {@code dev} and {@code test} profiles. */
  public static final String KNOWN_DEV_JWT_SECRET =
      "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=";

  private static final int MIN_JWT_KEY_BYTES = 32;

  private final Environment environment;

  public ProdSecurityStartupValidator(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void afterPropertiesSet() {
    validate(environment);
  }

  static void validate(Environment environment) {
    requirePresent(
        environment.getProperty("spring.datasource.url"),
        "Production requires spring.datasource.url (env DB_URL)");
    requirePresent(
        environment.getProperty("spring.datasource.username"),
        "Production requires spring.datasource.username (env DB_USERNAME)");
    requirePresent(
        environment.getProperty("spring.datasource.password"),
        "Production requires spring.datasource.password (env DB_PASSWORD)");

    String allowedOrigins =
        requirePresent(
            environment.getProperty("app.cors.allowed-origins"),
            "Production requires app.cors.allowed-origins (env ALLOWED_ORIGINS)");
    rejectLocalOrigins(allowedOrigins);

    if (!environment.getProperty("app.security.cookie.secure", Boolean.class, false)) {
      throw new IllegalStateException(
          "Production requires app.security.cookie.secure=true (secure auth cookies)");
    }

    String jwtSecret =
        requirePresent(
            environment.getProperty("app.security.jwt.secret-key"),
            "Production requires app.security.jwt.secret-key (env JWT_SECRET)");
    rejectKnownDevJwtSecret(jwtSecret);
    requireMinimumJwtKeyLength(jwtSecret);
  }

  private static String requirePresent(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(message);
    }
    return value;
  }

  private static void rejectKnownDevJwtSecret(String jwtSecret) {
    if (KNOWN_DEV_JWT_SECRET.equals(jwtSecret.strip())) {
      throw new IllegalStateException(
          "Production rejected a known development JWT secret; set JWT_SECRET to a unique,"
              + " high-entropy Base64 value");
    }
  }

  private static void requireMinimumJwtKeyLength(String jwtSecret) {
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(jwtSecret.strip());
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException(
          "Production JWT_SECRET must be valid Base64 encoding at least 256 bits of key material",
          ex);
    }
    if (keyBytes.length < MIN_JWT_KEY_BYTES) {
      throw new IllegalStateException(
          "Production JWT_SECRET must decode to at least 256 bits (32 bytes) of key material");
    }
  }

  private static void rejectLocalOrigins(String allowedOrigins) {
    String[] origins = allowedOrigins.split(",");
    for (String origin : origins) {
      String normalized = origin.strip().toLowerCase(Locale.ROOT);
      if (normalized.contains("localhost")) {
        throw new IllegalStateException(
            "Production app.cors.allowed-origins must not include localhost");
      }
      if (normalized.contains("127.0.0.1")) {
        throw new IllegalStateException(
            "Production app.cors.allowed-origins must not include 127.0.0.1");
      }
    }
  }
}
