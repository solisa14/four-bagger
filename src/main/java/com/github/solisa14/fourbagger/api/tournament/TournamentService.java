package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.user.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

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
            List.of(TournamentStatus.REGISTRATION, TournamentStatus.BRACKET_READY, TournamentStatus.IN_PROGRESS);
    private final TournamentRepository tournamentRepository;
    private final TournamentBracketService tournamentBracketService;
    private final TournamentBracketEligibilityPolicy bracketEligibilityPolicy;
    private final TournamentGameResultRepository tournamentGameResultRepository;
    private final TournamentMatchAuthorizationService authorizationService;

    /**
     * Constructs a new TournamentService with required dependencies.
     *
     * @param tournamentRepository the repository for tournament data access
     * @param tournamentBracketService the service for generating tournament brackets
     * @param bracketEligibilityPolicy the policy for bracket participant eligibility
     * @param tournamentGameResultRepository the repository for tournament game results
     * @param authorizationService the service for tournament access checks
     */
    public TournamentService(
            TournamentRepository tournamentRepository,
            TournamentBracketService tournamentBracketService,
            TournamentBracketEligibilityPolicy bracketEligibilityPolicy,
            TournamentGameResultRepository tournamentGameResultRepository,
            TournamentMatchAuthorizationService authorizationService) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentBracketService = tournamentBracketService;
        this.bracketEligibilityPolicy = bracketEligibilityPolicy;
        this.tournamentGameResultRepository = tournamentGameResultRepository;
        this.authorizationService = authorizationService;
    }

    private static String requireManualDisplayName(String rawDisplayName) {
        String displayName = rawDisplayName == null ? null : rawDisplayName.trim();
        if (displayName == null || displayName.isBlank()) {
            throw new InvalidTournamentStateException("Guest display name is required");
        }
        return displayName;
    }

    private static void assertUniqueManualName(Set<String> seen, String displayName) {
        String key = displayName.toLowerCase(Locale.ROOT);
        if (!seen.add(key)) {
            throw new InvalidTournamentStateException("Every guest must belong to exactly one complete team");
        }
    }

    private static void assertNotManualDoublesRosterMutation(Tournament tournament) {
        if (tournament.isManualDoubles()) {
            throw new InvalidTournamentStateException(
                    "Manual doubles rosters must be updated through complete team rows");
        }
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

        if (tournament.getParticipationMode() == TournamentParticipationMode.ORGANIZER_MANAGED) {
            throw new InvalidTournamentStateException("Organizer-managed tournaments do not allow account self-join");
        }

        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new InvalidTournamentStateException("Tournament is not open for registration");
        }

        boolean alreadyJoined =
                tournament.getParticipants().stream().anyMatch(participant -> participant.matchesUser(user.getId()));
        if (alreadyJoined) {
            throw new DuplicateTournamentParticipantException();
        }

        TournamentParticipant participant = TournamentParticipant.builder()
                .tournament(tournament)
                .user(user)
                .build();
        tournament.getParticipants().add(participant);
        tournamentRepository.save(tournament);
        return participant;
    }

    /**
     * Retrieves a tournament by its unique join code.
     *
     * @param joinCode the 6-character join code of the tournament to retrieve
     * @return the tournament
     * @throws TournamentNotFoundException if no tournament exists with that join code
     */
    @Transactional(readOnly = true)
    public Tournament getTournamentByJoinCode(String joinCode) {
        Tournament tournament =
                tournamentRepository.findDetailByJoinCode(joinCode).orElseThrow(TournamentNotFoundException::new);
        if (tournament.getParticipationMode() == TournamentParticipationMode.ORGANIZER_MANAGED) {
            throw new InvalidTournamentStateException("Organizer-managed tournaments do not support join-code lookup");
        }
        initializeTournamentDetails(tournament);
        return tournament;
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
        Tournament tournament = tournamentRepository.findById(id).orElseThrow(TournamentNotFoundException::new);
        initializeTournamentDetails(tournament);
        return tournament;
    }

    @Transactional(readOnly = true)
    public Tournament getTournamentForUser(UUID id, User currentUser) {
        Tournament tournament = tournamentRepository.findDetailById(id).orElseThrow(TournamentNotFoundException::new);
        authorizationService.authorizeTournamentAccess(currentUser, tournament);
        initializeTournamentDetails(tournament);
        return tournament;
    }

    @Transactional(readOnly = true)
    public ActiveTournaments listActiveTournamentsForUser(User currentUser) {
        List<Tournament> hosting = tournamentRepository.findByOrganizer_IdAndStatusInOrderByUpdatedAtDesc(
                currentUser.getId(), ACTIVE_STATUSES);
        Set<UUID> hostedIds = hosting.stream().map(Tournament::getId).collect(Collectors.toSet());
        List<Tournament> playing =
                tournamentRepository.findParticipatingActiveTournaments(currentUser.getId(), ACTIVE_STATUSES).stream()
                        .filter(tournament -> !hostedIds.contains(tournament.getId()))
                        .toList();
        return new ActiveTournaments(hosting, playing);
    }

    @Transactional(readOnly = true)
    public List<Tournament> listCompletedTournamentsForUser(User currentUser) {
        return tournamentRepository.findCompletedTournamentsForUser(currentUser.getId());
    }

    /**
     * Creates a new tournament with the given command. Self-join tournaments get a join code;
     * organizer-managed tournaments do not.
     *
     * @param command the command containing tournament details
     * @return the newly created tournament
     * @throws InvalidTournamentStateException if participation mode is missing
     * @throws JoinCodeGenerationException if a unique join code could not be generated
     */
    public Tournament createTournament(CreateTournamentCommand command) {
        if (command.participationMode() == null) {
            throw new InvalidTournamentStateException("A participation mode is required");
        }

        if (command.participationMode() == TournamentParticipationMode.ORGANIZER_MANAGED) {
            return tournamentRepository.save(buildTournament(command, null));
        }

        for (int attempt = 1; attempt <= MAX_JOIN_CODE_ATTEMPTS; attempt++) {
            String joinCode = generateJoinCode();
            Tournament tournament = buildTournament(command, joinCode);
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

    private Tournament buildTournament(CreateTournamentCommand command, String joinCode) {
        GameType gameType = command.gameType() != null ? command.gameType() : GameType.SINGLES;
        DoublesPairingMode doublesPairingMode = null;
        if (command.participationMode() == TournamentParticipationMode.ORGANIZER_MANAGED
                && gameType == GameType.DOUBLES) {
            doublesPairingMode = DoublesPairingMode.RANDOM;
        }
        return Tournament.builder()
                .organizer(command.organizer())
                .title(command.title())
                .status(TournamentStatus.REGISTRATION)
                .gameType(gameType)
                .format(command.format() != null ? command.format() : TournamentFormat.SINGLE_ELIMINATION)
                .participationMode(command.participationMode())
                .doublesPairingMode(doublesPairingMode)
                .joinCode(joinCode)
                .build();
    }

    /**
     * Deletes a tournament and all its associated data.
     *
     * @param id the UUID of the tournament to delete
     * @throws TournamentNotFoundException if the tournament does not exist
     */
    public void deleteTournament(UUID id, User currentUser) {
        Tournament tournament = tournamentRepository.findById(id).orElseThrow(TournamentNotFoundException::new);
        authorizeOrganizer(currentUser, tournament);
        tournamentGameResultRepository.deleteByTournamentId(id);
        // Teams FK to participants; clear match routing/teams before cascade-removing participants.
        clearBracketGraph(tournament);
        tournamentRepository.delete(tournament);
    }

    private void clearBracketGraph(Tournament tournament) {
        clearMatchGraph(tournament);
        tournament.getTeams().clear();
        tournamentRepository.flush();
    }

    private void clearMatchGraph(Tournament tournament) {
        List<Match> existingMatches = tournament.getRounds().stream()
                .flatMap(round -> round.getMatches().stream())
                .toList();

        existingMatches.forEach(match -> {
            match.setWinnerNextMatch(null);
            match.setWinnerNextMatchPosition(null);
            match.setLoserNextMatch(null);
            match.setLoserNextMatchPosition(null);
        });
        tournamentRepository.flush();

        tournament.getRounds().forEach(round -> round.getMatches().clear());
        tournamentRepository.flush();
    }

    private void authorizeOrganizer(User currentUser, Tournament tournament) {
        if (!tournament.getOrganizer().getId().equals(currentUser.getId())) {
            throw new TournamentAccessDeniedException(tournament.getId());
        }
    }

    private void initializeTournamentDetails(Tournament tournament) {
        tournament.getTeams().forEach(this::initializeTeam);
        tournament.getRounds().forEach(round -> {
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
        team.getPlayerOne().identityLabel();
        if (team.getPlayerTwo() != null) {
            team.getPlayerTwo().identityLabel();
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
     * <p>For organizer-managed manual doubles, existing pairs are preserved and only team seeds are
     * randomized. Random doubles (and all other modes) re-pair from the participant roster.
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

        boolean manualDoubles = tournament.isManualDoubles();

        if (tournament.getStatus() == TournamentStatus.BRACKET_READY) {
            if (manualDoubles) {
                clearMatchGraph(tournament);
            } else {
                clearBracketGraph(tournament);
            }
        } else if (!manualDoubles) {
            tournament.getTeams().clear();
        }

        if (manualDoubles) {
            assignSeedsToManualTeams(tournament);
        } else {
            List<TournamentParticipant> shuffledParticipants = new ArrayList<>(tournament.getParticipants());
            Collections.shuffle(shuffledParticipants, RANDOM);
            if (tournament.getGameType() == GameType.DOUBLES) {
                for (int i = 0; i < shuffledParticipants.size(); i += 2) {
                    TournamentTeam team = TournamentTeam.builder()
                            .tournament(tournament)
                            .playerOne(shuffledParticipants.get(i))
                            .playerTwo(shuffledParticipants.get(i + 1))
                            .seed((i / 2) + 1)
                            .build();
                    tournament.getTeams().add(team);
                }
            } else {
                for (int i = 0; i < shuffledParticipants.size(); i++) {
                    TournamentTeam team = TournamentTeam.builder()
                            .tournament(tournament)
                            .playerOne(shuffledParticipants.get(i))
                            .seed(i + 1)
                            .build();
                    tournament.getTeams().add(team);
                }
            }
        }

        tournamentBracketService.planBracket(tournament, tournament.getTeams());

        tournament.setStatus(TournamentStatus.BRACKET_READY);
        tournamentRepository.save(tournament);
    }

    private void assignSeedsToManualTeams(Tournament tournament) {
        List<TournamentTeam> teams = new ArrayList<>(tournament.getTeams());
        Collections.shuffle(teams, RANDOM);
        for (int i = 0; i < teams.size(); i++) {
            teams.get(i).setSeed(i + 1);
            teams.get(i).setLosses(0);
            teams.get(i).setEliminated(false);
        }
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
    public void updateRoundSettings(UUID tournamentId, User currentUser, int roundNumber, Integer bestOf) {
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

        List<TournamentRound> matchingRounds = tournament.getRounds().stream()
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
            throw new InvalidTournamentStateException("Tournament can only be started when bracket is ready");
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
     * Adds a guest participant to an organizer-managed tournament during registration.
     *
     * @param tournamentId the UUID of the tournament
     * @param currentUser the organizer performing the mutation
     * @param displayName the guest display name (trimmed; uniqueness is case-insensitive)
     * @return the created guest participant
     * @throws TournamentAccessDeniedException if the current user is not the organizer
     * @throws InvalidTournamentStateException if registration is closed, the tournament is not
     *     organizer-managed, or the name is blank
     * @throws DuplicateGuestDisplayNameException if the normalized name is already used
     */
    public TournamentParticipant addGuestParticipant(UUID tournamentId, User currentUser, String displayName) {
        Tournament tournament =
                tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
        authorizeOrganizer(currentUser, tournament);

        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new InvalidTournamentStateException("Cannot add guests after registration");
        }
        assertNotManualDoublesRosterMutation(tournament);

        TournamentParticipant guest = tournament.addGuestParticipant(displayName);
        tournamentRepository.saveAndFlush(tournament);

        // GenerationType.UUID is applied on flush; reload so the returned entity has its id.
        String persistedDisplayName = guest.getDisplayName();
        return tournamentRepository
                .findDetailById(tournamentId)
                .orElseThrow(TournamentNotFoundException::new)
                .getParticipants()
                .stream()
                .filter(TournamentParticipant::isGuest)
                .filter(participant -> persistedDisplayName.equals(participant.getDisplayName()))
                .findFirst()
                .orElseThrow(TournamentParticipantNotFoundException::new);
    }

    /**
     * Updates a guest participant's display name during registration.
     *
     * @param tournamentId the UUID of the tournament
     * @param currentUser the organizer performing the mutation
     * @param participantId the guest participant to rename
     * @param displayName the new display name
     * @return the updated guest participant
     */
    public TournamentParticipant updateGuestParticipant(
            UUID tournamentId, User currentUser, UUID participantId, String displayName) {
        Tournament tournament =
                tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
        authorizeOrganizer(currentUser, tournament);

        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new InvalidTournamentStateException("Cannot update guests after registration");
        }
        assertNotManualDoublesRosterMutation(tournament);

        TournamentParticipant guest = tournament.updateGuestDisplayName(participantId, displayName);
        tournamentRepository.save(tournament);
        return guest;
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
        assertNotManualDoublesRosterMutation(tournament);

        boolean removed =
                tournament.getParticipants().removeIf(participant -> participantId.equals(participant.getId()));
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
        authorizationService.authorizeTournamentAccess(currentUser, tournament);

        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new InvalidTournamentStateException("Cannot leave tournament after registration");
        }

        UUID currentUserId = currentUser.getId();
        boolean removed = tournament.getParticipants().removeIf(participant -> participant.matchesUser(currentUserId));
        if (!removed) {
            throw new TournamentParticipantNotFoundException();
        }

        tournamentRepository.save(tournament);
    }

    private boolean isValidBestOf(int bestOf) {
        return bestOf == 1 || bestOf == 3 || bestOf == 5 || bestOf == 7;
    }

    /**
     * Sets the doubles pairing mode for an organizer-managed doubles tournament during registration.
     * Changing the mode clears all guests and draft teams.
     *
     * @param tournamentId the tournament to update
     * @param currentUser the organizer
     * @param doublesPairingMode the new pairing mode
     */
    public void setDoublesPairingMode(UUID tournamentId, User currentUser, DoublesPairingMode doublesPairingMode) {
        Tournament tournament =
                tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
        authorizeOrganizer(currentUser, tournament);

        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new InvalidTournamentStateException("Cannot change doubles pairing mode after registration");
        }
        if (tournament.getParticipationMode() != TournamentParticipationMode.ORGANIZER_MANAGED) {
            throw new InvalidTournamentStateException(
                    "Doubles pairing mode is only available on organizer-managed tournaments");
        }
        if (tournament.getGameType() != GameType.DOUBLES) {
            throw new InvalidTournamentStateException("Doubles pairing mode is only available on doubles tournaments");
        }
        if (doublesPairingMode == null) {
            throw new InvalidTournamentStateException("A doubles pairing mode is required");
        }

        if (tournament.getDoublesPairingMode() != doublesPairingMode) {
            // Teams FK to participants — delete teams first, then guests (same order as clearBracketGraph).
            tournament.getTeams().clear();
            tournamentRepository.flush();
            tournament.getParticipants().clear();
            tournamentRepository.flush();
        }
        tournament.setDoublesPairingMode(doublesPairingMode);
        tournamentRepository.save(tournament);
    }

    /**
     * Replaces the guest roster and draft teams for a manual doubles tournament during registration.
     * Every guest display name must appear in exactly one complete team.
     *
     * @param tournamentId the tournament to update
     * @param currentUser the organizer
     * @param teams complete two-guest team rows
     */
    public void replaceManualTeams(UUID tournamentId, User currentUser, List<ManualTeamRow> teams) {
        Tournament tournament =
                tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
        authorizeOrganizer(currentUser, tournament);

        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new InvalidTournamentStateException("Cannot update manual teams after registration");
        }
        if (tournament.getParticipationMode() != TournamentParticipationMode.ORGANIZER_MANAGED) {
            throw new InvalidTournamentStateException(
                    "Manual doubles teams are only available on organizer-managed tournaments");
        }
        if (tournament.getGameType() != GameType.DOUBLES
                || tournament.getDoublesPairingMode() != DoublesPairingMode.MANUAL) {
            throw new InvalidTournamentStateException("Manual doubles teams require manual pairing mode");
        }
        if (teams == null || teams.isEmpty()) {
            throw new InvalidTournamentStateException("At least one manual team is required");
        }

        validateManualTeamRows(teams);

        // Teams FK to participants — delete teams first, then guests. Flush after each clear so:
        // 1) team deletes don't null non-nullable player FKs while removing guests
        // 2) guest deletes land before re-inserting the same display names (unique index)
        tournament.getTeams().clear();
        tournamentRepository.flush();
        tournament.getParticipants().clear();
        tournamentRepository.flush();

        for (ManualTeamRow row : teams) {
            TournamentParticipant playerOne = tournament.addGuestParticipant(row.playerOneDisplayName());
            TournamentParticipant playerTwo = tournament.addGuestParticipant(row.playerTwoDisplayName());
            tournament
                    .getTeams()
                    .add(TournamentTeam.builder()
                            .tournament(tournament)
                            .playerOne(playerOne)
                            .playerTwo(playerTwo)
                            .build());
        }

        tournamentRepository.save(tournament);
    }

    private void validateManualTeamRows(List<ManualTeamRow> teams) {
        Set<String> seen = new HashSet<>();
        for (ManualTeamRow row : teams) {
            String playerOne = requireManualDisplayName(row.playerOneDisplayName());
            String playerTwo = requireManualDisplayName(row.playerTwoDisplayName());
            if (playerOne.equalsIgnoreCase(playerTwo)) {
                throw new InvalidTournamentStateException("Each manual team must contain two different guests");
            }
            assertUniqueManualName(seen, playerOne);
            assertUniqueManualName(seen, playerTwo);
        }
    }
}
