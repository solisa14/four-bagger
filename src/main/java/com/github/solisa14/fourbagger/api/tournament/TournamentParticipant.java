package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

/**
 * Represents a user who has registered to participate in a specific tournament. In a singles
 * tournament, a participant directly maps to a team. In doubles, participants are combined to form
 * teams.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(
    name = "tournament_participants",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_tournament_participants_tournament_user",
          columnNames = {"tournament_id", "user_id"})
    })
public class TournamentParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
}
