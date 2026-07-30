package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Organizer-only service for overriding tournament match results and rewiring bracket slots. */
@Service
public class TournamentMatchOverrideService {

  private final TournamentMatchSupport matchSupport;
  private final TournamentGameResultRepository resultRepository;
  private final TournamentMatchAuthorizationService authorizationService;
  private final TournamentProgressionService progressionService;

  @Autowired
  TournamentMatchOverrideService(
      TournamentMatchSupport matchSupport,
      TournamentGameResultRepository resultRepository,
      TournamentMatchAuthorizationService authorizationService,
      TournamentProgressionService progressionService) {
    this.matchSupport = matchSupport;
    this.resultRepository = resultRepository;
    this.authorizationService = authorizationService;
    this.progressionService = progressionService;
  }

  public TournamentMatchOverrideService(
      TournamentRepository tournamentRepository,
      MatchRepository matchRepository,
      TournamentGameResultRepository resultRepository,
      TournamentMatchAuthorizationService authorizationService,
      TournamentProgressionService progressionService,
      TournamentMapper tournamentMapper) {
    this(
        new TournamentMatchSupport(
            tournamentRepository,
            matchRepository,
            resultRepository,
            tournamentMapper,
            progressionService),
        resultRepository,
        authorizationService,
        progressionService);
  }

  @Transactional
  public TournamentMatchDetailResponse overrideMatchResult(
      UUID tournamentId,
      UUID matchId,
      User currentUser,
      OverrideTournamentMatchResultRequest request) {
    Tournament tournament = matchSupport.requireTournament(tournamentId);
    Match match = matchSupport.requireMatch(matchId, tournamentId);
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

    match = matchSupport.requireMatch(matchId, tournamentId);
    return matchSupport.toDetail(match);
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
}
