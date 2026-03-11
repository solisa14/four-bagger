package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int MAX_JOIN_CODE_ATTEMPTS = 5;
  private final TournamentRepository tournamentRepository;

  public TournamentService(TournamentRepository tournamentRepository) {
    this.tournamentRepository = tournamentRepository;
  }

  public TournamentParticipant joinTournament(String joinCode, User user) {
    Tournament tournament =
        tournamentRepository.findByJoinCode(joinCode).orElseThrow(TournamentNotFoundException::new);

    if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
      throw new InvalidTournamentStateException("Tournament is not open for registration");
    }

    boolean alreadyJoined =
        tournament.getParticipants().stream()
            .anyMatch(participant -> user.getId().equals(participant.getUser().getId()));
    if (alreadyJoined) {
      throw new DuplicateTournamentParticipantException();
    }

    TournamentParticipant participant =
        TournamentParticipant.builder().tournament(tournament).user(user).build();
    tournament.getParticipants().add(participant);
    tournamentRepository.save(tournament);
    return participant;
  }

  public Tournament createTournament(User organizer, String title) {
    for (int attempt = 1; attempt <= MAX_JOIN_CODE_ATTEMPTS; attempt++) {
      String joinCode = generateJoinCode();
      Tournament tournament =
          Tournament.builder()
              .organizer(organizer)
              .title(title)
              .status(TournamentStatus.REGISTRATION)
              .joinCode(joinCode)
              .build();
      try {
        return tournamentRepository.save(tournament);
      } catch (DataIntegrityViolationException ex) {
        if (attempt == MAX_JOIN_CODE_ATTEMPTS) {
          throw new JoinCodeGenerationException();
        }
      }
    }
    throw new JoinCodeGenerationException();
  }

  public void deleteTournament(UUID id) {
    tournamentRepository.findById(id).orElseThrow(TournamentNotFoundException::new);
    tournamentRepository.deleteById(id);
  }

  public void generateBracket(UUID tournamentId) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);

    if (tournament.getStatus() != TournamentStatus.REGISTRATION
        && tournament.getStatus() != TournamentStatus.BRACKET_READY) {
      throw new InvalidTournamentStateException(
          "Cannot generate or reshuffle bracket unless tournament is in REGISTRATION or BRACKET_READY");
    }

    if (tournament.getParticipants().size() <= 2) {
      throw new InvalidTournamentStateException(
          "Cannot generate bracket with 2 or fewer participants");
    }

    List<TournamentParticipant> shuffledParticipants =
        new ArrayList<>(tournament.getParticipants());
    Collections.shuffle(shuffledParticipants, RANDOM);

    tournament.getTeams().clear();
    for (int i = 0; i < shuffledParticipants.size(); i++) {
      TournamentTeam team =
          TournamentTeam.builder()
              .tournament(tournament)
              .playerOne(shuffledParticipants.get(i).getUser())
              .seed(i + 1)
              .build();
      tournament.getTeams().add(team);
    }

    if (tournament.getRounds().isEmpty()) {
      int roundCount = calculateRoundCount(shuffledParticipants.size());
      for (int roundNumber = 1; roundNumber <= roundCount; roundNumber++) {
        TournamentRound round =
            TournamentRound.builder()
                .tournament(tournament)
                .roundNumber(roundNumber)
                .bestOf(1)
                .scoringMode(ScoringMode.STANDARD)
                .build();
        tournament.getRounds().add(round);
      }
    }

    tournament.setStatus(TournamentStatus.BRACKET_READY);
    tournamentRepository.save(tournament);
  }

  public void updateRoundSettings(
      UUID tournamentId, int roundNumber, Integer bestOf, ScoringMode scoringMode) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);

    if (tournament.getStatus() != TournamentStatus.BRACKET_READY) {
      throw new InvalidTournamentStateException(
          "Round settings can only be changed when tournament is BRACKET_READY");
    }

    if (roundNumber <= 0) {
      throw new InvalidRoundConfigurationException("Round number must be greater than 0");
    }

    if (bestOf == null && scoringMode == null) {
      throw new InvalidRoundConfigurationException("At least one round setting must be provided");
    }

    TournamentRound round =
        tournament.getRounds().stream()
            .filter(r -> roundNumber == r.getRoundNumber())
            .findFirst()
            .orElseThrow(TournamentRoundNotFoundException::new);

    if (bestOf != null) {
      if (!isValidBestOf(bestOf)) {
        throw new InvalidRoundConfigurationException("bestOf must be one of: 1, 3, 5, or 7");
      }
      round.setBestOf(bestOf);
    }

    if (scoringMode != null) {
      round.setScoringMode(scoringMode);
    }

    tournamentRepository.save(tournament);
  }

  private String generateJoinCode() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder sb = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
    }
    return sb.toString();
  }

  public void removeParticipant(UUID tournamentId, UUID participantId) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);

    if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
      throw new InvalidTournamentStateException("Cannot remove participants after registration");
    }

    boolean removed =
        tournament
            .getParticipants()
            .removeIf(participant -> participantId.equals(participant.getId()));
    if (!removed) {
      throw new TournamentParticipantNotFoundException();
    }

    tournamentRepository.save(tournament);
  }

  private boolean isValidBestOf(int bestOf) {
    return bestOf == 1 || bestOf == 3 || bestOf == 5 || bestOf == 7;
  }

  private int calculateRoundCount(int teamCount) {
    int rounds = 0;
    int bracketSize = 1;
    while (bracketSize < teamCount) {
      bracketSize *= 2;
      rounds++;
    }
    return rounds;
  }
}
