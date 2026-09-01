package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.FourBaggerApiApplication;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenStartupCleanupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void applicationStartup_repeatedlyRemovesOnlyExpiredRefreshTokens() {
        String suffix = UUID.randomUUID().toString();
        User expiredUser = userRepository.saveAndFlush(
                TestDataFactory.user(null, "expired-" + suffix, "encoded", Role.USER));
        User activeUser = userRepository.saveAndFlush(
                TestDataFactory.user(null, "active-" + suffix, "encoded", Role.USER));
        refreshTokenRepository.saveAndFlush(
                TestDataFactory.refreshToken(expiredUser, Instant.now().minusSeconds(60), "expired-" + suffix));
        refreshTokenRepository.saveAndFlush(
                TestDataFactory.refreshToken(activeUser, Instant.now().plusSeconds(600), "active-" + suffix));

        restartApplication();
        restartApplication();

        assertThat(refreshTokenRepository.findByTokenHash("expired-" + suffix)).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("active-" + suffix)).isPresent();
    }

    private void restartApplication() {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(FourBaggerApiApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.profiles.active=test")
                .run()) {
            // Startup behavior is the seam under test.
        }
    }

}
