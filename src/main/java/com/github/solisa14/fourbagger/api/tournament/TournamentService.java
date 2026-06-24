package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.user.User;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing the lifecycle of a tournament. This includes creation,
 * participant registration, bracket generation, and updating round configuration settings.
 */
@Service
@Transactional
public class TournamentService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int MAX_JOIN_CODE_ATTEMPTS = 10;
  private static final List<TournamentStatus> ACTIVE_STATUSES =
      List.of(
          TournamentStatus.REGISTRATION,
          TournamentStatus.BRACKET_READY,
          TournamentStatus.IN_PROGRESS);
  private final TournamentRepository tournamentRepository;
  private final TournamentBracketService tournamentBracketService;
  private final TournamentBracketEligibilityPolicy bracketEligibilityPolicy;

  /**
   * Constructs a new TournamentService with required dependencies.
   *
   * @param tournamentRepository the repository for tournament data access
   * @param tournamentBracketService the service for generating tournament brackets
   * @param bracketEligibilityPolicy the policy for bracket participant eligibility
   */
  public TournamentService(
      TournamentRepository tournamentRepository,
      TournamentBracketService tournamentBracketService,
      TournamentBracketEligibilityPolicy bracketEligibilityPolicy) {
    this.tournamentRepository = tournamentRepository;
    this.tournamentBracketService = tournamentBracketService;
    this.bracketEligibilityPolicy = bracketEligibilityPolicy;
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
  @Transactional(readOnly = true)
  public Tournament getTournament(UUID id) {
    Tournament tournament =
        tournamentRepository.findById(id).orElseThrow(TournamentNotFoundException::new);
    initializeTournamentDetails(tournament);
    return tournament;
  }

  @Transactional(readOnly = true)
  public Tournament getTournamentForUser(UUID id, User currentUser) {
    Tournament tournament =
        tournamentRepository.findDetailById(id).orElseThrow(TournamentNotFoundException::new);
    if (!canAccessTournament(currentUser, tournament)) {
      throw new TournamentAccessDeniedException(tournament.getId());
    }
    initializeTournamentDetails(tournament);
    return tournament;
  }

  @Transactional(readOnly = true)
  public ActiveTournaments listActiveTournamentsForUser(User currentUser) {
    List<Tournament> hosting =
        tournamentRepository.findByOrganizer_IdAndStatusInOrderByUpdatedAtDesc(
            currentUser.getId(), ACTIVE_STATUSES);
    Set<UUID> hostedIds = hosting.stream().map(Tournament::getId).collect(Collectors.toSet());
    List<Tournament> playing =
        tournamentRepository
            .findParticipatingActiveTournaments(currentUser.getId(), ACTIVE_STATUSES)
            .stream()
            .filter(tournament -> !hostedIds.contains(tournament.getId()))
            .toList();
    return new ActiveTournaments(hosting, playing);
  }

  @Transactional(readOnly = true)
  public List<Tournament> listCompletedTournamentsForUser(User currentUser) {
    return tournamentRepository.findCompletedTournamentsForUser(currentUser.getId());
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
              .gameType(command.gameType() != null ? command.gameType() : GameType.SINGLES)
              .format(
                  command.format() != null ? command.format() : TournamentFormat.SINGLE_ELIMINATION)
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
  public void deleteTournament(UUID id, User currentUser) {
    Tournament tournament =
        tournamentRepository.findById(id).orElseThrow(TournamentNotFoundException::new);
    authorizeOrganizer(currentUser, tournament);
    tournamentRepository.deleteById(id);
  }

  private void authorizeOrganizer(User currentUser, Tournament tournament) {
    if (!tournament.getOrganizer().getId().equals(currentUser.getId())) {
      throw new TournamentAccessDeniedException(tournament.getId());
    }
  }

  private boolean canAccessTournament(User currentUser, Tournament tournament) {
    if (currentUser == null) {
      return false;
    }

    UUID currentUserId = currentUser.getId();
    return tournament.getOrganizer().getId().equals(currentUserId)
        || tournament.getParticipants().stream()
            .anyMatch(participant -> participant.getUser().getId().equals(currentUserId));
  }

  private void initializeTournamentDetails(Tournament tournament) {
    tournament
        .getRounds()
        .forEach(
            round -> {
              round.getMatches().size();
              round.getMatches().forEach(this::initializeMatchDetails);
            });
  }

  private void initializeMatchDetails(Match match) {
    initializeTeam(match.getTeamOne());
    initializeTeam(match.getTeamTwo());
    initializeTeam(match.getWinner());
    initializeRoute(match.getWinnerNextMatch());
    initializeRoute(match.getLoserNextMatch());
  }

  private void initializeTeam(TournamentTeam team) {
    if (team == null) {
      return;
    }
    team.getSeed();
    team.getPlayerOne().getUsername();
    if (team.getPlayerTwo() != null) {
      team.getPlayerTwo().getUsername();
    }
  }

  private void initializeRoute(Match destination) {
    if (destination != null) {
      destination.getRound().getBracketType();
    }
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
  public void generateBracket(UUID tournamentId, User currentUser) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    authorizeOrganizer(currentUser, tournament);

    if (tournament.getStatus() != TournamentStatus.REGISTRATION
        && tournament.getStatus() != TournamentStatus.BRACKET_READY) {
      throw new InvalidTournamentStateException(
          "Cannot generate or reshuffle bracket unless tournament is in REGISTRATION or BRACKET_READY");
    }

    bracketEligibilityPolicy.validateForBracketGeneration(tournament);

    List<TournamentParticipant> shuffledParticipants =
        new ArrayList<>(tournament.getParticipants());
    Collections.shuffle(shuffledParticipants, RANDOM);

    if (tournament.getStatus() == TournamentStatus.BRACKET_READY) {
      prepareBracketForRegeneration(tournament);
    } else {
      tournament.getTeams().clear();
    }

    if (tournament.getGameType() == GameType.DOUBLES) {
      for (int i = 0; i < shuffledParticipants.size(); i += 2) {
        TournamentTeam team =
            TournamentTeam.builder()
                .tournament(tournament)
                .playerOne(shuffledParticipants.get(i).getUser())
                .playerTwo(shuffledParticipants.get(i + 1).getUser())
                .seed((i / 2) + 1)
                .build();
        tournament.getTeams().add(team);
      }
    } else {
      for (int i = 0; i < shuffledParticipants.size(); i++) {
        TournamentTeam team =
            TournamentTeam.builder()
                .tournament(tournament)
                .playerOne(shuffledParticipants.get(i).getUser())
                .seed(i + 1)
                .build();
        tournament.getTeams().add(team);
      }
    }

    tournamentBracketService.planBracket(tournament, tournament.getTeams());

    tournament.setStatus(TournamentStatus.BRACKET_READY);
    tournamentRepository.save(tournament);
  }

  private void prepareBracketForRegeneration(Tournament tournament) {
    List<Match> existingMatches =
        tournament.getRounds().stream().flatMap(round -> round.getMatches().stream()).toList();

    existingMatches.forEach(
        match -> {
          match.setWinnerNextMatch(null);
          match.setWinnerNextMatchPosition(null);
          match.setLoserNextMatch(null);
          match.setLoserNextMatchPosition(null);
        });
    tournamentRepository.flush();

    tournament.getRounds().forEach(round -> round.getMatches().clear());
    tournamentRepository.flush();

    tournament.getTeams().clear();
    tournamentRepository.flush();
  }

  /**
   * Updates the best-of series count for a specific round.
   *
   * @param tournamentId the UUID of the tournament
   * @param roundNumber the number of the round to configure
   * @param bestOf the number of games required to win a match in this round (must be 1, 3, 5, or 7)
   * @throws InvalidRoundConfigurationException if the parameters are invalid
   * @throws InvalidTournamentStateException if the tournament is not in the BRACKET_READY state
   */
  public void updateRoundSettings(
      UUID tournamentId, User currentUser, int roundNumber, Integer bestOf) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    authorizeOrganizer(currentUser, tournament);

    if (tournament.getStatus() != TournamentStatus.BRACKET_READY) {
      throw new InvalidTournamentStateException(
          "Round settings can only be changed when tournament is BRACKET_READY");
    }

    if (roundNumber <= 0) {
      throw new InvalidRoundConfigurationException("Round number must be greater than 0");
    }

    if (bestOf == null) {
      throw new InvalidRoundConfigurationException("bestOf must be provided");
    }

    List<TournamentRound> matchingRounds =
        tournament.getRounds().stream()
            .filter(r -> roundNumber == r.getRoundNumber())
            .toList();

    if (matchingRounds.isEmpty()) {
      throw new TournamentRoundNotFoundException();
    }

    if (!isValidBestOf(bestOf)) {
      throw new InvalidRoundConfigurationException("bestOf must be one of: 1, 3, 5, or 7");
    }
    matchingRounds.forEach(round -> round.setBestOf(bestOf));

    tournamentRepository.save(tournament);
  }

  /**
   * Transitions a tournament from BRACKET_READY to IN_PROGRESS, allowing matches to be played.
   *
   * @param tournamentId the UUID of the tournament
   * @throws InvalidTournamentStateException if the tournament bracket has not been generated
   */
  public void startTournament(UUID tournamentId, User currentUser) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    authorizeOrganizer(currentUser, tournament);

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
  public void removeParticipant(UUID tournamentId, User currentUser, UUID participantId) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    authorizeOrganizer(currentUser, tournament);

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

  /**
   * Removes the current user's participant registration during the registration phase.
   *
   * @param tournamentId the UUID of the tournament
   * @param currentUser the user withdrawing from the tournament
   * @throws TournamentNotFoundException if the tournament does not exist
   * @throws TournamentAccessDeniedException if the user cannot access the tournament
   * @throws InvalidTournamentStateException if registration has closed
   * @throws TournamentParticipantNotFoundException if the user is not a participant
   */
  public void leaveTournament(UUID tournamentId, User currentUser) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    if (!canAccessTournament(currentUser, tournament)) {
      throw new TournamentAccessDeniedException(tournament.getId());
    }

    if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
      throw new InvalidTournamentStateException("Cannot leave tournament after registration");
    }

    UUID currentUserId = currentUser.getId();
    boolean removed =
        tournament
            .getParticipants()
            .removeIf(participant -> participant.getUser().getId().equals(currentUserId));
    if (!removed) {
      throw new TournamentParticipantNotFoundException();
    }

    tournamentRepository.save(tournament);
  }

  private boolean isValidBestOf(int bestOf) {
    return bestOf == 1 || bestOf == 3 || bestOf == 5 || bestOf == 7;
  }
}
