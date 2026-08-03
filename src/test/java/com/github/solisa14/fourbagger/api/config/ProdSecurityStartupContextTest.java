package com.github.solisa14.fourbagger.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ProdSecurityStartupContextTest {

    private static final String STRONG_JWT_SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProdSecurityStartupValidator.class)
            .withPropertyValues("spring.profiles.active=prod");

    private static String[] validDatasourceAndOrigins() {
        return new String[] {
            "spring.datasource.url=jdbc:postgresql://db.example.com:5432/fourbagger?sslmode=require",
            "spring.datasource.username=fourbagger",
            "spring.datasource.password=s3cret",
            "app.cors.allowed-origins=https://app.example.com",
            "app.security.cookie.secure=true"
        };
    }

    @Test
    void contextFails_whenJwtSecretIsKnownDevPlaceholder() {
        contextRunner
                .withPropertyValues(validDatasourceAndOrigins())
                .withPropertyValues("app.security.jwt.secret-key=" + ProdSecurityStartupValidator.KNOWN_DEV_JWT_SECRET)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessageContaining("known")
                            .hasMessageContaining("JWT");
                });
    }

    @Test
    void contextFails_whenRequiredDbUrlIsMissing() {
        contextRunner
                .withPropertyValues(
                        "app.security.jwt.secret-key=" + STRONG_JWT_SECRET,
                        "app.cors.allowed-origins=https://app.example.com",
                        "spring.datasource.username=fourbagger",
                        "spring.datasource.password=s3cret")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessageContaining("DB_URL");
                });
    }

    @Test
    void contextFails_whenJwtSecretIsMissing() {
        contextRunner.withPropertyValues(validDatasourceAndOrigins()).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause()
                    .hasMessageContaining("JWT");
        });
    }

    @Test
    void contextLoads_whenProdConfigurationIsValid() {
        contextRunner
                .withPropertyValues(validDatasourceAndOrigins())
                .withPropertyValues("app.security.jwt.secret-key=" + STRONG_JWT_SECRET)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(ProdSecurityStartupValidator.class));
    }
}
