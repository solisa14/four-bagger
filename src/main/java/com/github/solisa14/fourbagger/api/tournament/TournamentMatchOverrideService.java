package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Organizer-only service for overriding tournament match results and rewiring bracket slots. */
@Service
public class TournamentMatchOverrideService {

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;
  private final TournamentGameResultRepository resultRepository;
  private final TournamentMatchAuthorizationService authorizationService;
  private final TournamentProgressionService progressionService;
  private final TournamentMapper tournamentMapper;

  public TournamentMatchOverrideService(
      TournamentRepository tournamentRepository,
      MatchRepository matchRepository,
      TournamentGameResultRepository resultRepository,
      TournamentMatchAuthorizationService authorizationService,
      TournamentProgressionService progressionService,
      TournamentMapper tournamentMapper) {
    this.tournamentRepository = tournamentRepository;
    this.matchRepository = matchRepository;
    this.resultRepository = resultRepository;
    this.authorizationService = authorizationService;
    this.progressionService = progressionService;
    this.tournamentMapper = tournamentMapper;
  }

  @Transactional
  public TournamentMatchDetailResponse overrideMatchResult(
      UUID tournamentId,
      UUID matchId,
      User currentUser,
      OverrideTournamentMatchResultRequest request) {
    Tournament tournament = loadTournament(tournamentId);
    Match match = loadMatch(matchId, tournamentId);
    authorizationService.authorizeOrganizer(currentUser, tournament);
    validateOverrideAllowed(tournament, match, request);

    if (match.getStatus() == MatchStatus.COMPLETED) {
      progressionService.revertMatchCompletion(match);
    }

    resultRepository.deleteByMatchId(match.getId());

    TournamentTeam winningTeam = resolveWinningTeam(match, request.winnerTeamId());
    TournamentTeam losingTeam = resolveLosingTeam(match, winningTeam);
    int[] canonicalScores = deriveCanonicalScores(match, winningTeam);

    progressionService.applyMatchOverride(
        match, winningTeam, losingTeam, canonicalScores[0], canonicalScores[1]);

    match = loadMatch(matchId, tournamentId);
    return buildDetail(match);
  }

  private void validateOverrideAllowed(
      Tournament tournament, Match match, OverrideTournamentMatchResultRequest request) {
    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
      throw new InvalidTournamentStateException(
          "Cannot override match results unless the tournament is IN_PROGRESS");
    }
    if (match.isBye()) {
      throw new InvalidTournamentStateException("Cannot override results for a bye match");
    }
    if (match.getTeamOne() == null || match.getTeamTwo() == null) {
      throw new InvalidTournamentStateException("Both teams must be assigned before overriding");
    }
    if (match.getStatus() != MatchStatus.IN_PROGRESS
        && match.getStatus() != MatchStatus.COMPLETED) {
      throw new InvalidTournamentStateException(
          "Cannot override match results unless the match is IN_PROGRESS or COMPLETED");
    }

    validateWinnerTeam(match, request.winnerTeamId());
    validateDownstreamNotStarted(match);
  }

  private void validateWinnerTeam(Match match, UUID winnerTeamId) {
    UUID teamOneId = match.getTeamOne().getId();
    UUID teamTwoId = match.getTeamTwo().getId();

    if (!winnerTeamId.equals(teamOneId) && !winnerTeamId.equals(teamTwoId)) {
      throw new InvalidTournamentStateException("Winner team is not a participant in the match");
    }
  }

  private int[] deriveCanonicalScores(Match match, TournamentTeam winningTeam) {
    int winnerWins = progressionService.winsToClinch(match);
    if (winningTeam.getId().equals(match.getTeamOne().getId())) {
      return new int[] {winnerWins, 0};
    }
    return new int[] {0, winnerWins};
  }

  private void validateDownstreamNotStarted(Match match) {
    validateDestinationNotLocked(match.getWinnerNextMatch());
    validateDestinationNotLocked(match.getLoserNextMatch());
  }

  private void validateDestinationNotLocked(Match destination) {
    if (destination == null) {
      return;
    }
    if (destination.isBye() && destination.getStatus() == MatchStatus.COMPLETED) {
      validateDestinationNotLocked(destination.getWinnerNextMatch());
      return;
    }
    if (destination.getStartedAt() != null
        || destination.getStatus() == MatchStatus.IN_PROGRESS
        || destination.getStatus() == MatchStatus.COMPLETED) {
      throw new InvalidTournamentStateException(
          "Cannot override match after downstream play has started");
    }
  }

  private TournamentTeam resolveWinningTeam(Match match, UUID winnerTeamId) {
    if (match.getTeamOne().getId().equals(winnerTeamId)) {
      return match.getTeamOne();
    }
    return match.getTeamTwo();
  }

  private TournamentTeam resolveLosingTeam(Match match, TournamentTeam winningTeam) {
    if (winningTeam.getId().equals(match.getTeamOne().getId())) {
      return match.getTeamTwo();
    }
    return match.getTeamOne();
  }

  private Tournament loadTournament(UUID tournamentId) {
    return tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
  }

  private Match loadMatch(UUID matchId, UUID tournamentId) {
    Match match =
        matchRepository
            .findForResponseById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));
    UUID ownerTournamentId = match.getRound().getTournament().getId();
    if (!tournamentId.equals(ownerTournamentId)) {
      throw new InvalidTournamentStateException("Match does not belong to this tournament");
    }
    return match;
  }

  private TournamentMatchDetailResponse buildDetail(Match match) {
    List<TournamentGameResult> results = resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId());
    return tournamentMapper.toMatchDetailResponse(
        match, results, progressionService.nextGameNumber(match));
  }
}
