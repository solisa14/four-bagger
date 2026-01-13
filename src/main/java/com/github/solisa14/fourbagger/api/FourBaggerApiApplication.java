package com.github.solisa14.fourbagger.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot application entry point for the FourBagger API.
 *
 * <p>Bootstraps the REST API server with auto-configuration for web, JPA, and security components.
 * Enables scanning for {@link org.springframework.boot.context.properties.ConfigurationProperties}
 * annotated classes throughout the application to bind external configuration properties.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FourBaggerApiApplication {

  /**
   * Launches the Spring Boot application.
   *
   * @param args command line arguments passed to the application
   */
  static void main(String[] args) {
    SpringApplication.run(FourBaggerApiApplication.class, args);
  }
}
