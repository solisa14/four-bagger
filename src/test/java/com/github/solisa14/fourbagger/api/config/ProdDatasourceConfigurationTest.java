package com.github.solisa14.fourbagger.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProdDatasourceConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    void prodProfile_usesSmallIdleFriendlyTlsConnectionPool() {
        contextRunner.run(context -> {
            assertThat(context.getEnvironment().getProperty("spring.datasource.hikari.maximum-pool-size"))
                    .isEqualTo("5");
            assertThat(context.getEnvironment().getProperty("spring.datasource.hikari.minimum-idle"))
                    .isEqualTo("0");
            assertThat(context.getEnvironment().getProperty("spring.datasource.hikari.idle-timeout"))
                    .isEqualTo("60000");
            assertThat(context.getEnvironment().getProperty("spring.datasource.hikari.connection-timeout"))
                    .isEqualTo("30000");
            assertThat(context.getEnvironment().getProperty("spring.datasource.hikari.data-source-properties.sslmode"))
                    .isEqualTo("require");
        });
    }
}
