package com.github.solisa14.fourbagger.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.application.security.jwt.secret-key=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=",
    "spring.application.security.jwt.refresh-token.expiration-ms=60000"
})
@Transactional
class RefreshTokenRepositoryTest {

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @Autowired
  private UserRepository userRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .username("testuser")
        .email("test@example.com")
        .password("password")
        .role(Role.USER)
        .build();
    userRepository.save(testUser);
  }

  @Test
  void findByToken_ShouldReturnToken_WhenExists() {
    RefreshToken token = RefreshToken.builder()
        .token(UUID.randomUUID().toString())
        .expiryDate(Instant.now().plusMillis(600000))
        .user(testUser)
        .build();
    refreshTokenRepository.save(token);

    Optional<RefreshToken> found = refreshTokenRepository.findByToken(token.getToken());

    assertThat(found).isPresent();
    assertThat(found.get().getUser().getUsername()).isEqualTo("testuser");
  }

  @Test
  void deleteByToken_ShouldRemoveToken() {
    String tokenValue = UUID.randomUUID().toString();
    RefreshToken token = RefreshToken.builder()
        .token(tokenValue)
        .expiryDate(Instant.now().plusMillis(600000))
        .user(testUser)
        .build();
    refreshTokenRepository.save(token);

    refreshTokenRepository.deleteByToken(tokenValue);

    Optional<RefreshToken> found = refreshTokenRepository.findByToken(tokenValue);
    assertThat(found).isEmpty();
  }

  @Test
  void deleteByUser_ShouldRemoveAllTokensForUser() {
    RefreshToken token1 = RefreshToken.builder()
        .token(UUID.randomUUID().toString())
        .expiryDate(Instant.now().plusMillis(600000))
        .user(testUser)
        .build();
    RefreshToken token2 = RefreshToken.builder()
        .token(UUID.randomUUID().toString())
        .expiryDate(Instant.now().plusMillis(600000))
        .user(testUser)
        .build();
    refreshTokenRepository.save(token1);
    refreshTokenRepository.save(token2);

    refreshTokenRepository.deleteByUser(testUser);

    assertThat(refreshTokenRepository.findAll()).isEmpty();
  }
}
