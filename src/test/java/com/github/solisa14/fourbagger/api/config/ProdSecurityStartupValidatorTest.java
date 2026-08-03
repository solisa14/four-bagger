package com.github.solisa14.fourbagger.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdSecurityStartupValidatorTest {

    private static final String STRONG_JWT_SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    private static MockEnvironment validProdEnvironment() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.security.jwt.secret-key", STRONG_JWT_SECRET);
        env.setProperty("app.cors.allowed-origins", "https://app.example.com");
        env.setProperty("app.security.cookie.secure", "true");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://db.example.com:5432/fourbagger?sslmode=require");
        env.setProperty("spring.datasource.username", "fourbagger");
        env.setProperty("spring.datasource.password", "s3cret");
        return env;
    }

    @Test
    void validate_whenConfigurationIsValid_doesNotThrow() {
        MockEnvironment env = validProdEnvironment();

        assertThatCode(() -> ProdSecurityStartupValidator.validate(env)).doesNotThrowAnyException();
    }

    @Test
    void validate_whenJwtSecretMissing_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("app.security.jwt.secret-key", "");

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT");
    }

    @Test
    void validate_whenJwtSecretIsKnownDevPlaceholder_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("app.security.jwt.secret-key", ProdSecurityStartupValidator.KNOWN_DEV_JWT_SECRET);

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("known")
                .hasMessageContaining("JWT");
    }

    @Test
    void validate_whenJwtSecretDecodesBelow256Bits_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("app.security.jwt.secret-key", Base64.getEncoder().encodeToString("tooshort".getBytes()));

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256");
    }

    @Test
    void validate_whenAllowedOriginsContainLocalhost_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("app.cors.allowed-origins", "https://app.example.com,http://localhost:3000");

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
    }

    @Test
    void validate_whenAllowedOriginsContainLoopback_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("app.cors.allowed-origins", "http://127.0.0.1:8080");

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("127.0.0.1");
    }

    @Test
    void validate_whenDbUrlMissing_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("spring.datasource.url", " ");

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_URL")
                .hasMessageContaining("spring.datasource.url");
    }

    @Test
    void validate_whenDbUsernameMissing_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("spring.datasource.username", "");

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_USERNAME");
    }

    @Test
    void validate_whenDbPasswordMissing_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("spring.datasource.password", "");

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void validate_whenAllowedOriginsMissing_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("app.cors.allowed-origins", "");

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALLOWED_ORIGINS");
    }

    @Test
    void validate_whenCookieSecureIsFalse_failsWithClearError() {
        MockEnvironment env = validProdEnvironment();
        env.setProperty("app.security.cookie.secure", "false");

        assertThatThrownBy(() -> ProdSecurityStartupValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cookie.secure");
    }
}
