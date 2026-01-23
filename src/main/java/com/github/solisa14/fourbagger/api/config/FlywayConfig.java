package com.github.solisa14.fourbagger.api.config;

import java.util.Arrays;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class FlywayConfig {

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
