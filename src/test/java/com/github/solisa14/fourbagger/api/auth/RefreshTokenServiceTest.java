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
import org.mockito.ArgumentCaptor;
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
  void issueRefreshToken_createsSessionWhenUserHasNoExistingToken() {
    UUID userId = UUID.randomUUID();
    User user = TestDataFactory.user(userId, "user1", "user1@example.com", "encoded", Role.USER);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());
    when(refreshTokenRepository.saveAndFlush(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Instant now = Instant.now();
    RefreshTokenSession session = refreshTokenService.issueRefreshToken(userId);

    assertThat(session.user()).isEqualTo(user);
    assertThat(session.rawToken()).isNotBlank();

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).saveAndFlush(captor.capture());
    RefreshToken savedToken = captor.getValue();
    assertThat(savedToken.getUser()).isEqualTo(user);
    assertThat(savedToken.getTokenHash()).isEqualTo(refreshTokenService.hashToken(session.rawToken()));
    assertThat(savedToken.getExpiryDate()).isAfter(now);
  }

  @Test
  void issueRefreshToken_replacesExistingUserSession() {
    UUID userId = UUID.randomUUID();
    User user = TestDataFactory.user(userId, "user1", "user1@example.com", "encoded", Role.USER);
    RefreshToken existing =
        RefreshToken.builder()
            .user(user)
            .tokenHash("existing-hash")
            .expiryDate(Instant.now().plusSeconds(30))
            .build();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
    when(refreshTokenRepository.saveAndFlush(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefreshTokenSession session = refreshTokenService.issueRefreshToken(userId);

    assertThat(session.user()).isEqualTo(user);
    assertThat(session.rawToken()).isNotBlank();
    assertThat(existing.getTokenHash()).isEqualTo(refreshTokenService.hashToken(session.rawToken()));
    assertThat(existing.getExpiryDate()).isAfter(now());
  }

  @Test
  void rotateRefreshToken_rotatesWhenValid() {
    User user =
        TestDataFactory.user(UUID.randomUUID(), "user1", "user1@example.com", "e", Role.USER);
    String oldToken = "old-token";
    RefreshToken existing =
        RefreshToken.builder()
            .user(user)
            .tokenHash(refreshTokenService.hashToken(oldToken))
            .expiryDate(Instant.now().plusSeconds(60))
            .build();
    when(refreshTokenRepository.findByTokenHashForUpdate(refreshTokenService.hashToken(oldToken)))
        .thenReturn(Optional.of(existing));
    when(refreshTokenRepository.saveAndFlush(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefreshTokenSession rotated = refreshTokenService.rotateRefreshToken(oldToken);

    assertThat(rotated.user()).isEqualTo(user);
    assertThat(rotated.rawToken()).isNotBlank();
    assertThat(existing.getTokenHash()).isEqualTo(refreshTokenService.hashToken(rotated.rawToken()));
    verify(refreshTokenRepository).saveAndFlush(existing);
  }

  @Test
  void rotateRefreshToken_throwsWhenExpired() {
    User user =
        TestDataFactory.user(UUID.randomUUID(), "user1", "user1@example.com", "e", Role.USER);
    String oldToken = "old-token";
    RefreshToken existing =
        RefreshToken.builder()
            .user(user)
            .tokenHash(refreshTokenService.hashToken(oldToken))
            .expiryDate(Instant.now().minusSeconds(10))
            .build();
    when(refreshTokenRepository.findByTokenHashForUpdate(refreshTokenService.hashToken(oldToken)))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(oldToken))
        .isInstanceOf(TokenRefreshException.class);
    verify(refreshTokenRepository).delete(existing);
  }

  @Test
  void deleteByUserId_deletesWhenUserFound() {
    UUID userId = UUID.randomUUID();

    refreshTokenService.deleteByUserId(userId);

    verify(refreshTokenRepository).deleteByUserId(userId);
  }

  @Test
  void deleteByToken_hashesBeforeDeleting() {
    refreshTokenService.deleteByToken("token");

    verify(refreshTokenRepository).deleteByTokenHash(refreshTokenService.hashToken("token"));
  }

  @Test
  void purgeExpiredTokens_deletesExpired() {
    refreshTokenService.purgeExpiredTokens();

    verify(refreshTokenRepository).deleteByExpiryDateLessThan(any(Instant.class));
  }

  private Instant now() {
    return Instant.now().minusSeconds(1);
  }
}
