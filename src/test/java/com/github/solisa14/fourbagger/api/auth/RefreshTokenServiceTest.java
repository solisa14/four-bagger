package com.github.solisa14.fourbagger.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.solisa14.fourbagger.api.common.exception.TokenRefreshException;
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

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private UserRepository userRepository;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    refreshTokenService = new RefreshTokenService(refreshTokenRepository, userRepository);
    ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 60000L);
  }

  @Test
  void createRefreshToken_ShouldReturnNewToken() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

    RefreshToken token = refreshTokenService.createRefreshToken(userId);

    assertThat(token).isNotNull();
    assertThat(token.getToken()).isNotNull();
    assertThat(token.getUser()).isEqualTo(user);
    assertThat(token.getExpiryDate()).isAfter(Instant.now());
  }

  @Test
  void verifyExpiration_ShouldThrowException_WhenExpired() {
    RefreshToken token = new RefreshToken();
    token.setToken("expired-token");
    token.setExpiryDate(Instant.now().minusMillis(1000));

    assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
        .isInstanceOf(TokenRefreshException.class)
        .hasMessageContaining("expired");
        
    verify(refreshTokenRepository).delete(token);
  }

  @Test
  void verifyExpiration_ShouldReturnToken_WhenValid() {
    RefreshToken token = new RefreshToken();
    token.setToken("valid-token");
    token.setExpiryDate(Instant.now().plusMillis(1000));

    RefreshToken result = refreshTokenService.verifyExpiration(token);

    assertThat(result).isEqualTo(token);
  }
}
