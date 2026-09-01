package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.testsupport.AbstractDataJpaTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenRepositoryTest extends AbstractDataJpaTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByTokenHash_whenTokenExists_returnsToken() {
        User user = userRepository.saveAndFlush(TestDataFactory.user(null, "user1", "encoded", Role.USER));
        RefreshToken token = refreshTokenRepository.saveAndFlush(
                TestDataFactory.refreshToken(user, Instant.now().plusSeconds(60), "hash-1"));

        assertThat(refreshTokenRepository.findByTokenHash("hash-1")).contains(token);
    }

    @Test
    void deleteByTokenHash_whenTokenExists_removesToken() {
        User user = userRepository.saveAndFlush(TestDataFactory.user(null, "user2", "encoded", Role.USER));
        refreshTokenRepository.saveAndFlush(
                TestDataFactory.refreshToken(user, Instant.now().plusSeconds(60), "hash-2"));

        refreshTokenRepository.deleteByTokenHash("hash-2");

        assertThat(refreshTokenRepository.findByTokenHash("hash-2")).isEmpty();
    }

    @Test
    void deleteByExpiryDateLessThan_whenTokensAreExpired_removesExpiredTokens() {
        User user = userRepository.saveAndFlush(TestDataFactory.user(null, "user4", "encoded", Role.USER));
        refreshTokenRepository.saveAndFlush(
                TestDataFactory.refreshToken(user, Instant.now().minusSeconds(60), "hash-4"));

        refreshTokenRepository.deleteByExpiryDateLessThan(Instant.now());

        assertThat(refreshTokenRepository.findByTokenHash("hash-4")).isEmpty();
    }

    @Test
    void save_whenUserAlreadyHasActiveSession_throwsDataIntegrityViolationException() {
        User user = userRepository.saveAndFlush(TestDataFactory.user(null, "user5", "encoded", Role.USER));
        refreshTokenRepository.saveAndFlush(
                TestDataFactory.refreshToken(user, Instant.now().plusSeconds(60), "hash-5"));

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(
                        TestDataFactory.refreshToken(user, Instant.now().plusSeconds(120), "hash-6")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
