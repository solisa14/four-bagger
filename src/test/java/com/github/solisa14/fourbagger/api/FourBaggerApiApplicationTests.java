package com.github.solisa14.fourbagger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Integration test suite for the FourBagger API application.
 *
 * <p>Validates Spring Boot application startup and bean configuration using the full application
 * context.
 */
class FourBaggerApiApplicationTests extends AbstractIntegrationTest {

  @Autowired private Flyway flyway;

  /** Smoke test ensuring the Spring application context initializes without errors. */
  @Test
  void contextLoads() {}

  @Test
  void migrations_whenApplicationStarts_haveNoPendingMigrations() {
    assertThat(flyway.info().current()).isNotNull();
    assertThat(flyway.info().pending()).isEmpty();
    assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
  }

  @Test
  void migrate_whenSchemaIsAlreadyCurrent_appliesNothing() {
    MigrateResult result = flyway.migrate();

    assertThat(result.migrationsExecuted).isZero();
  }

  @Test
  void application_whenStartedAgainstAlreadyMigratedDatabase_startsWithoutPendingMigrations() {
    try (ConfigurableApplicationContext restartedContext =
        new SpringApplicationBuilder(FourBaggerApiApplication.class)
            .web(WebApplicationType.NONE)
            .properties("spring.profiles.active=test")
            .run()) {
      Flyway restartedFlyway = restartedContext.getBean(Flyway.class);

      assertThat(restartedFlyway.info().current()).isNotNull();
      assertThat(restartedFlyway.info().pending()).isEmpty();
      assertThat(restartedFlyway.getConfiguration().isBaselineOnMigrate()).isFalse();
    }
  }
}
