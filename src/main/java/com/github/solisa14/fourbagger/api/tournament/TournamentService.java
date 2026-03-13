package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing the lifecycle of a tournament. This includes creation,
 * participant registration, bracket generation, and updating round configuration settings.
 */
@Service
public class TournamentService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int MAX_JOIN_CODE_ATTEMPTS = 5;
  private final TournamentRepository tournamentRepository;
  private final TournamentBracketService tournamentBracketService;

  /**
   * Constructs a new TournamentService with required dependencies.
   *
   * @param tournamentRepository the repository for tournament data access
   * @param tournamentBracketService the service for generating tournament brackets
   */
  public TournamentService(
      TournamentRepository tournamentRepository,
      TournamentBracketService tournamentBracketService) {
    this.tournamentRepository = tournamentRepository;
    this.tournamentBracketService = tournamentBracketService;
  }

  /**
   * Allows a user to join a tournament using a unique join code.
   *
   * @param joinCode the 6-character code of the tournament to join
   * @param user the user attempting to join
   * @return the created participant record
   * @throws TournamentNotFoundException if no tournament matches the join code
   * @throws InvalidTournamentStateException if the tournament is not in REGISTRATION state
   * @throws DuplicateTournamentParticipantException if the user has already joined
   */
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

  /**
   * Retrieves a tournament by its ID.
   *
   * @param id the UUID of the tournament
   * @return the tournament
   * @throws TournamentNotFoundException if no tournament exists with that ID
   */
  public Tournament getTournament(UUID id) {
    return tournamentRepository.findById(id).orElseThrow(TournamentNotFoundException::new);
  }

  /**
   * Creates a new tournament with the given command and a randomly generated join code.
   *
   * @param command the command containing tournament details
   * @return the newly created tournament
   * @throws JoinCodeGenerationException if a unique join code could not be generated
   */
  public Tournament createTournament(CreateTournamentCommand command) {
    for (int attempt = 1; attempt <= MAX_JOIN_CODE_ATTEMPTS; attempt++) {
      String joinCode = generateJoinCode();
      Tournament tournament =
          Tournament.builder()
              .organizer(command.organizer())
              .title(command.title())
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

  /**
   * Deletes a tournament and all its associated data.
   *
   * @param id the UUID of the tournament to delete
   * @throws TournamentNotFoundException if the tournament does not exist
   */
  public void deleteTournament(UUID id) {
    tournamentRepository.findById(id).orElseThrow(TournamentNotFoundException::new);
    tournamentRepository.deleteById(id);
  }

  /**
   * Generates or regenerates the tournament bracket based on current participants. Participants are
   * randomly shuffled and seeded before generating matchups. The tournament transitions to the
   * BRACKET_READY state.
   *
   * @param tournamentId the UUID of the tournament
   * @throws TournamentNotFoundException if the tournament does not exist
   * @throws InvalidTournamentStateException if the tournament has already started or has too few
   *     participants
   */
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

    tournamentBracketService.planBracket(tournament, tournament.getTeams());

    tournament.setStatus(TournamentStatus.BRACKET_READY);
    tournamentRepository.save(tournament);
  }

  /**
   * Updates the rules (best-of series count and scoring mode) for a specific round.
   *
   * @param tournamentId the UUID of the tournament
   * @param roundNumber the number of the round to configure
   * @param bestOf the number of games required to win a match in this round (must be 1, 3, 5, or 7)
   * @param scoringMode the scoring mode rules for the round
   * @throws InvalidRoundConfigurationException if the parameters are invalid
   * @throws InvalidTournamentStateException if the tournament is not in the BRACKET_READY state
   */
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

  /**
   * Transitions a tournament from BRACKET_READY to IN_PROGRESS, allowing matches to be played.
   *
   * @param tournamentId the UUID of the tournament
   * @throws InvalidTournamentStateException if the tournament bracket has not been generated
   */
  public void startTournament(UUID tournamentId) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);

    if (tournament.getStatus() != TournamentStatus.BRACKET_READY) {
      throw new InvalidTournamentStateException(
          "Tournament can only be started when bracket is ready");
    }

    tournament.setStatus(TournamentStatus.IN_PROGRESS);
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

  /**
   * Removes a participant from a tournament before registration closes.
   *
   * @param tournamentId the UUID of the tournament
   * @param participantId the UUID of the participant to remove
   * @throws InvalidTournamentStateException if the tournament is no longer in the REGISTRATION
   *     phase
   * @throws TournamentParticipantNotFoundException if the participant does not exist in this
   *     tournament
   */
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
}
