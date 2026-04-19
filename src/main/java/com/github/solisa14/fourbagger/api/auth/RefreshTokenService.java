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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing refresh tokens, including creation, rotation, and deletion. */
@Service
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;

  @Value("${app.security.jwt.refresh-token.expiration-ms}")
  private Long refreshTokenDurationMs;

  /**
   * Constructs a RefreshTokenService.
   *
   * @param refreshTokenRepository the repository for refresh tokens
   * @param userRepository the repository for users
   */
  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.userRepository = userRepository;
  }

  /**
   * Finds a refresh token by its raw token string.
   *
   * @param token the raw refresh token
   * @return an Optional containing the refresh token if found
   */
  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenRepository.findByTokenHash(hashToken(token));
  }

  /**
   * Issues a new refresh token for a user.
   *
   * @param userId the UUID of the user
   * @return the newly created refresh token session
   */
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

    refreshTokenRepository.save(refreshToken);
    return new RefreshTokenSession(user, rawToken);
  }

  /**
   * Rotates a given refresh token, replacing it with a new one.
   *
   * @param token the raw refresh token to rotate
   * @return the new refresh token session
   * @throws TokenRefreshException if the token is not found or has expired
   */
  @Transactional
  public RefreshTokenSession rotateRefreshToken(String token) {
    RefreshToken oldToken =
        refreshTokenRepository
            .findByTokenHash(hashToken(token))
            .orElseThrow(
                () -> new TokenRefreshException("Refresh token is not in database"));

    verifyExpiration(oldToken);

    String newRawToken = generateRawToken();
    updateRefreshToken(oldToken, hashToken(newRawToken), calculateExpiryDate());
    refreshTokenRepository.save(oldToken);
    return new RefreshTokenSession(oldToken.getUser(), newRawToken);
  }

  /**
   * Verifies that a refresh token has not expired.
   *
   * @param token the refresh token entity to check
   * @return the token if it is valid
   * @throws TokenRefreshException if the token has expired
   */
  public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(token);
      throw new TokenRefreshException(
          "Refresh token was expired. Please make a new signin request");
    }
    return token;
  }

  /**
   * Deletes all refresh tokens for a user by their user ID.
   *
   * @param userId the UUID of the user
   */
  @Transactional
  public void deleteByUserId(UUID userId) {
    refreshTokenRepository.deleteByUserId(userId);
  }

  /**
   * Deletes a specific refresh token by its raw token string.
   *
   * @param token the raw refresh token
   */
  @Transactional
  public void deleteByToken(String token) {
    refreshTokenRepository.deleteByTokenHash(hashToken(token));
  }

  /**
   * Purges all expired refresh tokens from the database. Runs automatically at midnight every day.
   */
  @Transactional
  @Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
  public void purgeExpiredTokens() {
    refreshTokenRepository.deleteByExpiryDateLessThan(Instant.now());
  }

  private User loadUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new TokenRefreshException("Unable to refresh token"));
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
