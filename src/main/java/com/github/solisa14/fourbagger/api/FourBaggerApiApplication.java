package com.github.solisa14.fourbagger.api;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot application entry point for the FourBagger API.
 *
 * <p>Bootstraps the REST API server with auto-configuration for web, JPA, and security components.
 * Runs Flyway database migrations before the Spring context starts so JPA schema validation always
 * sees the expected tables.
 */
@SpringBootApplication
@EnableScheduling
public class FourBaggerApiApplication {

  private static final Logger log = LoggerFactory.getLogger(FourBaggerApiApplication.class);

  /**
   * Launches the Spring Boot application after applying Flyway migrations.
   *
   * @param args command line arguments passed to the application
   */
  public static void main(String[] args) {
    new SpringApplicationBuilder(FourBaggerApiApplication.class)
        .initializers(ctx -> runFlywayMigrations(ctx.getEnvironment()))
        .run(args);
  }

  /**
   * Runs Flyway migrations against the configured datasource before any Spring beans are created.
   * This avoids dependency-graph ordering issues with Hibernate's schema validation.
   */
  private static void runFlywayMigrations(Environment env) {
    String url = env.getProperty("spring.datasource.url");
    String user = env.getProperty("spring.datasource.username");
    String password = env.getProperty("spring.datasource.password");

    if (url == null || url.isBlank()) {
      log.info("Skipping Flyway: spring.datasource.url is not configured");
      return;
    }

    log.info("Running Flyway migrations against {}", url);
    Flyway flyway =
        Flyway.configure()
            .dataSource(url, user, password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();
    var result = flyway.migrate();
    log.info(
        "Flyway migration complete: {} migration(s) applied, schema version now {}",
        result.migrationsExecuted,
        result.targetSchemaVersion);
  }
}
