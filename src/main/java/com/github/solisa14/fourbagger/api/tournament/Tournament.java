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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Represents a cornhole tournament. A tournament goes through various states, from registration to
 * completion. It contains participants, teams formed from those participants, and a bracket
 * organized into rounds and matches.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(access = AccessLevel.PACKAGE)
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

  @Column(nullable = false, unique = true)
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

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;

  public static Tournament create(
      User organizer, String title, String joinCode, GameType gameType, TournamentFormat format) {
    return Tournament.builder()
        .organizer(organizer)
        .title(title)
        .joinCode(joinCode)
        .status(TournamentStatus.REGISTRATION)
        .gameType(gameType)
        .format(format)
        .build();
  }

  public static Tournament restore(
      UUID id,
      User organizer,
      String title,
      String joinCode,
      TournamentStatus status,
      GameType gameType) {
    return Tournament.builder()
        .id(id)
        .organizer(organizer)
        .title(title)
        .joinCode(joinCode)
        .status(status)
        .gameType(gameType)
        .build();
  }

  public List<TournamentParticipant> getParticipants() {
    return Collections.unmodifiableList(participants);
  }

  public List<TournamentTeam> getTeams() {
    return Collections.unmodifiableList(teams);
  }

  public List<TournamentRound> getRounds() {
    return Collections.unmodifiableList(rounds);
  }

  public void addParticipant(TournamentParticipant participant) {
    participant.assignTournament(this);
    participants.add(participant);
  }

  public boolean removeParticipant(UUID participantId) {
    TournamentParticipant participant =
        participants.stream()
            .filter(candidate -> participantId.equals(candidate.getId()))
            .findFirst()
            .orElse(null);
    if (participant == null) {
      return false;
    }
    participants.remove(participant);
    participant.detachTournament(this);
    return true;
  }

  public void replaceTeams(List<TournamentTeam> replacementTeams) {
    List<TournamentTeam> replacements = List.copyOf(replacementTeams);
    teams.forEach(team -> team.detachTournament(this));
    teams.clear();
    replacements.forEach(this::addTeam);
  }

  public void addTeam(TournamentTeam team) {
    team.assignTournament(this);
    teams.add(team);
  }

  public void addRound(TournamentRound round) {
    round.assignTournament(this);
    rounds.add(round);
  }

  public boolean removeRound(TournamentRound round) {
    if (!rounds.remove(round)) {
      return false;
    }
    round.detachTournament(this);
    return true;
  }

  public void markBracketReady() {
    status = TournamentStatus.BRACKET_READY;
  }

  public void start() {
    if (status != TournamentStatus.BRACKET_READY) {
      throw new InvalidTournamentStateException(
          "Tournament can only be started when bracket is ready");
    }
    status = TournamentStatus.IN_PROGRESS;
  }

  public void complete() {
    status = TournamentStatus.COMPLETED;
  }
}
