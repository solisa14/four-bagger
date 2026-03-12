package com.github.solisa14.fourbagger.api.config;

import java.util.Arrays;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Flyway database migrations.
 *
 * <p>Ensures that Flyway migrations are executed before the JPA entity manager factory is created,
 * allowing Hibernate to validate or interact with the migrated schema.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class FlywayConfig {

  /**
   * Post-processor to ensure the entity manager factory depends on Flyway.
   *
   * @return the bean factory post-processor
   */
  @Bean
  public static BeanFactoryPostProcessor flywayDependsOnPostProcessor() {
    return beanFactory -> {
      if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
        return;
      }
      var beanDefinition = beanFactory.getBeanDefinition("entityManagerFactory");
      String[] dependsOn = beanDefinition.getDependsOn();
      if (dependsOn == null) {
        beanDefinition.setDependsOn("flyway");
        return;
      }
      boolean alreadyDependsOnFlyway = Arrays.asList(dependsOn).contains("flyway");
      if (!alreadyDependsOnFlyway) {
        String[] updatedDependsOn = Arrays.copyOf(dependsOn, dependsOn.length + 1);
        updatedDependsOn[updatedDependsOn.length - 1] = "flyway";
        beanDefinition.setDependsOn(updatedDependsOn);
      }
    };
  }

  /**
   * Configures and initializes Flyway with the provided data source and locations.
   *
   * @param dataSource the data source to use for migrations
   * @param locations the locations of the migration scripts
   * @return the configured Flyway instance
   */
  @Bean(initMethod = "migrate")
  public Flyway flyway(
      DataSource dataSource,
      @Value("${spring.flyway.locations:classpath:db/migration}") String locations) {
    String[] locationArray =
        Arrays.stream(locations.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    return Flyway.configure().dataSource(dataSource).locations(locationArray).load();
  }
}
