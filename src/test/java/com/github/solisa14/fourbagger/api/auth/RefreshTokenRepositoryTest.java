package com.github.solisa14.fourbagger.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

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
  void findByToken_returnsToken() {
    User user = userRepository.saveAndFlush(createUser("user1", "user1@example.com"));
    RefreshToken token =
        refreshTokenRepository.saveAndFlush(
            createToken(user, "token-1", Instant.now().plusSeconds(60)));

    assertThat(refreshTokenRepository.findByToken("token-1")).contains(token);
  }

  @Test
  void deleteByToken_removesToken() {
    User user = userRepository.saveAndFlush(createUser("user2", "user2@example.com"));
    refreshTokenRepository.saveAndFlush(
        createToken(user, "token-2", Instant.now().plusSeconds(60)));

    refreshTokenRepository.deleteByToken("token-2");

    assertThat(refreshTokenRepository.findByToken("token-2")).isEmpty();
  }

  @Test
  void deleteByUser_removesUserTokens() {
    User user = userRepository.saveAndFlush(createUser("user3", "user3@example.com"));
    refreshTokenRepository.saveAndFlush(
        createToken(user, "token-3", Instant.now().plusSeconds(60)));

    refreshTokenRepository.deleteByUser(user);

    assertThat(refreshTokenRepository.findByToken("token-3")).isEmpty();
  }

  @Test
  void deleteByExpiryDateLessThan_removesExpiredTokens() {
    User user = userRepository.saveAndFlush(createUser("user4", "user4@example.com"));
    refreshTokenRepository.saveAndFlush(
        createToken(user, "token-4", Instant.now().minusSeconds(60)));

    refreshTokenRepository.deleteByExpiryDateLessThan(Instant.now());

    assertThat(refreshTokenRepository.findByToken("token-4")).isEmpty();
  }

  private User createUser(String username, String email) {
    return User.builder()
        .username(username)
        .email(email)
        .password("encoded")
        .firstName("Test")
        .lastName("User")
        .role(Role.USER)
        .build();
  }

  private RefreshToken createToken(User user, String token, Instant expiry) {
    return RefreshToken.builder().user(user).token(token).expiryDate(expiry).build();
  }
}
