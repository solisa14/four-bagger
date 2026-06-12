package com.github.solisa14.fourbagger.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.solisa14.fourbagger.api.testsupport.AbstractDataJpaTest;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RefreshTokenRepositoryTest extends AbstractDataJpaTest {

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private UserRepository userRepository;

  @Test
  void findByTokenHash_whenTokenExists_returnsToken() {
    User user = userRepository.saveAndFlush(createUser("user1"));
    RefreshToken token =
        refreshTokenRepository.saveAndFlush(
            createToken(user, "hash-1", Instant.now().plusSeconds(60)));

    assertThat(refreshTokenRepository.findByTokenHash("hash-1")).contains(token);
  }

  @Test
  void deleteByTokenHash_whenTokenExists_removesToken() {
    User user = userRepository.saveAndFlush(createUser("user2"));
    refreshTokenRepository.saveAndFlush(createToken(user, "hash-2", Instant.now().plusSeconds(60)));

    refreshTokenRepository.deleteByTokenHash("hash-2");

    assertThat(refreshTokenRepository.findByTokenHash("hash-2")).isEmpty();
  }

  @Test
  void deleteByExpiryDateLessThan_whenTokensAreExpired_removesExpiredTokens() {
    User user = userRepository.saveAndFlush(createUser("user4"));
    refreshTokenRepository.saveAndFlush(
        createToken(user, "hash-4", Instant.now().minusSeconds(60)));

    refreshTokenRepository.deleteByExpiryDateLessThan(Instant.now());

    assertThat(refreshTokenRepository.findByTokenHash("hash-4")).isEmpty();
  }

  @Test
  void save_whenUserAlreadyHasActiveSession_throwsDataIntegrityViolationException() {
    User user = userRepository.saveAndFlush(createUser("user5"));
    refreshTokenRepository.saveAndFlush(createToken(user, "hash-5", Instant.now().plusSeconds(60)));

    assertThatThrownBy(
            () ->
                refreshTokenRepository.saveAndFlush(
                    createToken(user, "hash-6", Instant.now().plusSeconds(120))))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  private User createUser(String username) {
    return User.builder()
        .username(username)
        .password("encoded")
        .firstName("Test")
        .lastName("User")
        .role(Role.USER)
        .build();
  }

  private RefreshToken createToken(User user, String tokenHash, Instant expiry) {
    return RefreshToken.builder().user(user).tokenHash(tokenHash).expiryDate(expiry).build();
  }
}
