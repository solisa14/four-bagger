package com.github.solisa14.fourbagger.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test suite for the FourBagger API application.
 *
 * <p>Validates Spring Boot application startup and bean configuration using the full application
 * context.
 */
@SpringBootTest(
    properties = {
      "spring.application.security.jwt.secret-key=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
    })
class FourBaggerApiApplicationTests {

  /** Smoke test ensuring the Spring application context initializes without errors. */
  @Test
  void contextLoads() {}
}
