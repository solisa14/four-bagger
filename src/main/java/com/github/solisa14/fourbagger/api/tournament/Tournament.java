package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Represents a cornhole tournament. A tournament goes through various states, from registration to
 * completion. It contains participants, teams formed from those participants, and a bracket
 * organized into rounds and matches.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "tournaments")
public class Tournament {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organizer_id", nullable = false)
  private User organizer;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private TournamentStatus status;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private GameType gameType = GameType.SINGLES;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private TournamentFormat format = TournamentFormat.SINGLE_ELIMINATION;

  @Column(nullable = false)
  private String title;

  @Column(name = "participation_mode", nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  @Setter(lombok.AccessLevel.NONE)
  private TournamentParticipationMode participationMode;

  /** Present for {@link TournamentParticipationMode#SELF_JOIN}; null for organizer-managed. */
  @Column(unique = true)
  private String joinCode;

  @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<TournamentParticipant> participants = new ArrayList<>();

  @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<TournamentTeam> teams = new ArrayList<>();

  @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<TournamentRound> rounds = new ArrayList<>();

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  /**
   * Adds a guest participant with a required display name unique within this tournament after trim
   * and case-insensitive comparison. Preserves the organizer's chosen casing for display.
   *
   * @param rawDisplayName organizer-entered name
   * @return the created guest participant attached to this tournament
   * @throws InvalidTournamentStateException if this is not an organizer-managed tournament or the
   *     name is blank
   * @throws DuplicateGuestDisplayNameException if the normalized name is already used
   */
  public TournamentParticipant addGuestParticipant(String rawDisplayName) {
    if (participationMode != TournamentParticipationMode.ORGANIZER_MANAGED) {
      throw new InvalidTournamentStateException(
          "Guest participants are only allowed on organizer-managed tournaments");
    }
    String displayName = rawDisplayName == null ? null : rawDisplayName.trim();
    if (displayName == null || displayName.isBlank()) {
      throw new InvalidTournamentStateException("Guest display name is required");
    }
    String key = displayName.toLowerCase(Locale.ROOT);
    boolean duplicate =
        participants.stream()
            .filter(TournamentParticipant::isGuest)
            .anyMatch(
                guest -> key.equals(guest.getDisplayName().toLowerCase(Locale.ROOT)));
    if (duplicate) {
      throw new DuplicateGuestDisplayNameException(displayName);
    }
    TournamentParticipant guest =
        TournamentParticipant.builder().tournament(this).displayName(displayName).build();
    participants.add(guest);
    return guest;
  }

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;
}
