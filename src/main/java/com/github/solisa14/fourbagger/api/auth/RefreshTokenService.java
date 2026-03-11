package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.common.exception.TokenRefreshException;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
    return refreshTokenRepository.findByTokenHash(hashToken(token));
  }

  @Transactional
  public RefreshTokenSession issueRefreshToken(UUID userId) {
    User user = loadUser(userId);
    String rawToken = generateRawToken();
    String tokenHash = hashToken(rawToken);
    Instant expiryDate = calculateExpiryDate();

    RefreshToken refreshToken =
        refreshTokenRepository
            .findByUserId(userId)
            .map(existingToken -> updateRefreshToken(existingToken, tokenHash, expiryDate))
            .orElseGet(() -> buildRefreshToken(user, tokenHash, expiryDate));

    try {
      refreshTokenRepository.saveAndFlush(refreshToken);
    } catch (DataIntegrityViolationException ex) {
      RefreshToken existingToken =
          refreshTokenRepository.findByUserId(userId).orElseThrow(() -> ex);
      updateRefreshToken(existingToken, tokenHash, expiryDate);
      refreshTokenRepository.saveAndFlush(existingToken);
    }

    return new RefreshTokenSession(user, rawToken);
  }

  @Transactional
  public RefreshTokenSession rotateRefreshToken(String token) {
    RefreshToken oldToken =
        refreshTokenRepository
            .findByTokenHashForUpdate(hashToken(token))
            .orElseThrow(
                () -> new TokenRefreshException(token, "Refresh token is not in database!"));

    verifyExpiration(oldToken);

    String newRawToken = generateRawToken();
    updateRefreshToken(oldToken, hashToken(newRawToken), calculateExpiryDate());
    refreshTokenRepository.saveAndFlush(oldToken);
    return new RefreshTokenSession(oldToken.getUser(), newRawToken);
  }

  public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(token);
      throw new TokenRefreshException(
          "Refresh token was expired. Please make a new signin request");
    }
    return token;
  }

  @Transactional
  public void deleteByUserId(UUID userId) {
    refreshTokenRepository.deleteByUserId(userId);
  }

  @Transactional
  public void deleteByToken(String token) {
    refreshTokenRepository.deleteByTokenHash(hashToken(token));
  }

  @Transactional
  @Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
  public void purgeExpiredTokens() {
    refreshTokenRepository.deleteByExpiryDateLessThan(Instant.now());
  }

  private User loadUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new TokenRefreshException("User not found with id: " + userId));
  }

  private RefreshToken buildRefreshToken(User user, String tokenHash, Instant expiryDate) {
    return RefreshToken.builder().user(user).tokenHash(tokenHash).expiryDate(expiryDate).build();
  }

  private RefreshToken updateRefreshToken(
      RefreshToken refreshToken, String tokenHash, Instant expiryDate) {
    refreshToken.setTokenHash(tokenHash);
    refreshToken.setExpiryDate(expiryDate);
    return refreshToken;
  }

  private Instant calculateExpiryDate() {
    return Instant.now().plusMillis(refreshTokenDurationMs);
  }

  private String generateRawToken() {
    return UUID.randomUUID().toString();
  }

  String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 must be available", e);
    }
  }
}
