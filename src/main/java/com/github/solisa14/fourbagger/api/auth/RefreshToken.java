package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Entity representing a refresh token used to obtain new access tokens.
 *
 * <p>Stores a one-way hash of the token, expiration time, and the associated user. Only one active
 * refresh session is allowed per user.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
