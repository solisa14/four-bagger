package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A participant in a specific tournament. Either an account holder ({@link #user}) or a
 * tournament-scoped guest ({@link #displayName}).
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "tournament_participants")
public class TournamentParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

  /** Present for self-join account participants; null for guest participants. */
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "user_id")
  private User user;

  /** Present for guest participants; null for account participants. */
  @Column(name = "display_name")
  private String displayName;

  public boolean isGuest() {
    return user == null;
  }

  public boolean matchesUser(UUID userId) {
    return !isGuest() && userId != null && userId.equals(user.getId());
  }

  public String identityLabel() {
    if (isGuest()) {
      return displayName;
    }
    return user.getUsername();
  }
}
