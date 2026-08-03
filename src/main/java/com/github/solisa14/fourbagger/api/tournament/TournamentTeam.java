package com.github.solisa14.fourbagger.api.tournament;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a competing entity within a tournament. A team can consist of a single participant (for
 * singles tournaments) or two participants (for doubles). Members are {@link TournamentParticipant}s
 * so both account holders and guests can compete.
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

    /** The first participant on the team. Required for all teams. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_one_participant_id", nullable = false)
    private TournamentParticipant playerOne;

    /** The second participant on the team. Nullable for singles tournaments. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_two_participant_id")
    private TournamentParticipant playerTwo;

    @Column(name = "losses")
    @Builder.Default
    private int losses = 0;

    @Column(name = "is_eliminated")
    @Builder.Default
    private boolean isEliminated = false;

    /** The seed number assigned to the team during bracket generation. */
    @Column
    private Integer seed;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
