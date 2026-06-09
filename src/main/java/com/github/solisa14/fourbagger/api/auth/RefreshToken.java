package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity representing a refresh token used to obtain new access tokens.
 *
 * <p>
 * Stores a one-way hash of the token, expiration time, and the associated user. Only one active
 * refresh session is allowed per user.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Builder(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiryDate;

  @OneToOne
  @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
  private User user;

  static RefreshToken issue(User user, String tokenHash, Instant expiryDate) {
    return RefreshToken.builder().user(user).tokenHash(tokenHash).expiryDate(expiryDate).build();
  }

  public static RefreshToken restore(User user, String tokenHash, Instant expiryDate) {
    return issue(user, tokenHash, expiryDate);
  }

  public void rotate(String tokenHash, Instant expiryDate) {
    if (tokenHash == null || tokenHash.isBlank() || expiryDate == null) {
      throw new IllegalArgumentException("Token hash and expiry date are required");
    }
    this.tokenHash = tokenHash;
    this.expiryDate = expiryDate;
  }
}
