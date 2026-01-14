package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.common.exception.TokenRefreshException;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;

  @Value("${app.security.jwt.refresh-token.expiration-ms}")
  private Long refreshTokenDurationMs;

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.userRepository = userRepository;
  }

  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
  }

  @Transactional
  public RefreshToken createRefreshToken(UUID userId) {
    RefreshToken refreshToken = new RefreshToken();

    refreshToken.setUser(
        userRepository
            .findById(userId)
            .orElseThrow(() -> new TokenRefreshException("User not found with id: " + userId)));
    refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
    refreshToken.setToken(UUID.randomUUID().toString());

    return refreshTokenRepository.save(refreshToken);
  }

  @Transactional
  public RefreshToken rotateRefreshToken(String token) {
    RefreshToken oldToken =
        findByToken(token)
            .orElseThrow(
                () -> new TokenRefreshException(token, "Refresh token is not in database!"));

    verifyExpiration(oldToken);
    deleteByToken(token);
    return createRefreshToken(oldToken.getUser().getId());
  }

  public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(token);
      throw new TokenRefreshException(
          token.getToken(), "Refresh token was expired. Please make a new signin request");
    }
    return token;
  }

  @Transactional
  public void deleteByUserId(UUID userId) {
    userRepository.findById(userId).ifPresent(refreshTokenRepository::deleteByUser);
  }

  @Transactional
  public void deleteByToken(String token) {
    refreshTokenRepository.deleteByToken(token);
  }

  @Transactional
  @Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
  public void purgeExpiredTokens() {
    refreshTokenRepository.deleteByExpiryDateLessThan(Instant.now());
  }
}
