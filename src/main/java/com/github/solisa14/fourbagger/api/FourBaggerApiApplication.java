package com.github.solisa14.fourbagger.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot application entry point for the FourBagger API.
 *
 * <p>Bootstraps the REST API server with auto-configuration for web, JPA, and security components.
 */
@SpringBootApplication
@EnableScheduling
public class FourBaggerApiApplication {

  /**
   * Launches the Spring Boot application.
   *
   * @param args command line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.run(FourBaggerApiApplication.class, args);
  }
}
