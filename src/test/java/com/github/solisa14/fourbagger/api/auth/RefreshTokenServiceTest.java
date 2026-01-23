package com.github.solisa14.fourbagger.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.solisa14.fourbagger.api.common.exception.TokenRefreshException;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private UserRepository userRepository;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    refreshTokenService = new RefreshTokenService(refreshTokenRepository, userRepository);
    ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 60000L);
  }

  @Test
  void createRefreshToken_setsFieldsAndSaves() {
    UUID userId = UUID.randomUUID();
    User user = TestDataFactory.user(userId, "user1", "user1@example.com", "encoded", Role.USER);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Instant now = Instant.now();
    RefreshToken token = refreshTokenService.createRefreshToken(userId);

    assertThat(token.getUser()).isEqualTo(user);
    assertThat(token.getToken()).isNotBlank();
    assertThat(token.getExpiryDate()).isAfter(now);
  }

  @Test
  void rotateRefreshToken_rotatesWhenValid() {
    User user =
        TestDataFactory.user(UUID.randomUUID(), "user1", "user1@example.com", "e", Role.USER);
    RefreshToken existing =
        TestDataFactory.refreshToken(user, Instant.now().plusSeconds(60), "old-token");
    when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(existing));

    RefreshTokenService spyService = spy(refreshTokenService);
    RefreshToken newToken =
        TestDataFactory.refreshToken(user, Instant.now().plusSeconds(120), "new-token");
    doReturn(newToken).when(spyService).createRefreshToken(user.getId());

    RefreshToken rotated = spyService.rotateRefreshToken("old-token");

    assertThat(rotated.getToken()).isEqualTo("new-token");
    verify(refreshTokenRepository).deleteByToken("old-token");
  }

  @Test
  void rotateRefreshToken_throwsWhenExpired() {
    User user =
        TestDataFactory.user(UUID.randomUUID(), "user1", "user1@example.com", "e", Role.USER);
    RefreshToken existing =
        TestDataFactory.refreshToken(user, Instant.now().minusSeconds(10), "old-token");
    when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("old-token"))
        .isInstanceOf(TokenRefreshException.class);
    verify(refreshTokenRepository).delete(existing);
  }

  @Test
  void deleteByUserId_deletesWhenUserFound() {
    UUID userId = UUID.randomUUID();
    User user = TestDataFactory.user(userId, "user1", "user1@example.com", "e", Role.USER);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    refreshTokenService.deleteByUserId(userId);

    verify(refreshTokenRepository).deleteByUser(user);
  }

  @Test
  void deleteByUserId_noopWhenUserMissing() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    refreshTokenService.deleteByUserId(userId);

    verify(refreshTokenRepository, never()).deleteByUser(any(User.class));
  }

  @Test
  void deleteByToken_delegatesToRepository() {
    refreshTokenService.deleteByToken("token");

    verify(refreshTokenRepository).deleteByToken("token");
  }

  @Test
  void purgeExpiredTokens_deletesExpired() {
    refreshTokenService.purgeExpiredTokens();

    verify(refreshTokenRepository).deleteByExpiryDateLessThan(any(Instant.class));
  }
}
