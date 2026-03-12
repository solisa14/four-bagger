package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

/**
 * Represents a competing entity within a tournament. A team can consist of a single player (for
 * singles tournaments) or two players (for doubles).
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "tournament_teams")
public class TournamentTeam {

  /** The unique identifier for the team. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** The tournament this team belongs to. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

  /** The first player on the team. Required for all teams. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_one_id", nullable = false)
  private User playerOne;

  /** The second player on the team. Nullable for singles tournaments. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_two_id")
  private User playerTwo;

  /** The seed number assigned to the team during bracket generation. */
  @Column private Integer seed;
}
