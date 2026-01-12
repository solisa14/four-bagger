package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.common.exception.TokenRefreshException;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

  @Value("${spring.application.security.jwt.refresh-token.expiration-ms}")
  private Long refreshTokenDurationMs;

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;

  public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.userRepository = userRepository;
  }

  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
  }

  @Transactional
  public RefreshToken createRefreshToken(UUID userId) {
    RefreshToken refreshToken = new RefreshToken();

    refreshToken.setUser(userRepository.findById(userId).orElseThrow(() -> 
        new TokenRefreshException("User not found with id: " + userId)));
    refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
    refreshToken.setToken(UUID.randomUUID().toString());

    return refreshTokenRepository.save(refreshToken);
  }

  public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(token);
      throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
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
}
