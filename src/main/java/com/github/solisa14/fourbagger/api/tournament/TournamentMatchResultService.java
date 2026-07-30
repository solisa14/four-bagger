package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Submits tournament physical game results and drives series progression. */
@Service
public class TournamentMatchResultService {

  private final TournamentMatchSupport matchSupport;
  private final TournamentGameResultRepository resultRepository;
  private final TournamentMatchAuthorizationService authorizationService;
  private final FinalScoreValidator finalScoreValidator;
  private final TournamentProgressionService progressionService;

  @Autowired
  TournamentMatchResultService(
      TournamentMatchSupport matchSupport,
      TournamentGameResultRepository resultRepository,
      TournamentMatchAuthorizationService authorizationService,
      FinalScoreValidator finalScoreValidator,
      TournamentProgressionService progressionService) {
    this.matchSupport = matchSupport;
    this.resultRepository = resultRepository;
    this.authorizationService = authorizationService;
    this.finalScoreValidator = finalScoreValidator;
    this.progressionService = progressionService;
  }

  public TournamentMatchResultService(
      TournamentRepository tournamentRepository,
      MatchRepository matchRepository,
      TournamentGameResultRepository resultRepository,
      TournamentMatchAuthorizationService authorizationService,
      FinalScoreValidator finalScoreValidator,
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
        finalScoreValidator,
        progressionService);
  }

  @Transactional
  public TournamentMatchDetailResponse submitResult(
      UUID tournamentId,
      UUID matchId,
      int gameNumber,
      User currentUser,
      SubmitTournamentGameResultRequest request) {
    Tournament tournament = matchSupport.requireTournament(tournamentId);
    Match match = matchSupport.requireMatch(matchId, tournamentId);
    authorizationService.authorizeMatchMutation(currentUser, tournament, match);
    validateMatchReady(match);

    Optional<TournamentGameResult> existingResult =
        resultRepository.findByMatchIdAndGameNumber(matchId, gameNumber);
    if (existingResult.isPresent()) {
      return handleExistingResult(existingResult.get(), request, match);
    }

    // After idempotency: exact/conflicting retries must still work when the tournament is no longer live.
    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
      throw new InvalidTournamentStateException(
          "Cannot submit results unless the tournament is IN_PROGRESS");
    }

    Integer expectedGameNumber = progressionService.nextGameNumber(match);
    if (expectedGameNumber == null) {
      throw new InvalidTournamentStateException("Match series is already clinched");
    }
    if (gameNumber != expectedGameNumber) {
      throw new InvalidTournamentStateException(
          "Game number must be " + expectedGameNumber + ", got " + gameNumber);
    }

    finalScoreValidator.validateScores(request.teamOneScore(), request.teamTwoScore());
    TournamentTeam winnerTeam =
        request.teamOneScore() > request.teamTwoScore()
            ? match.getTeamOne()
            : match.getTeamTwo();

    TournamentGameResult result =
        TournamentGameResult.builder()
            .match(match)
            .gameNumber(gameNumber)
            .winnerTeam(winnerTeam)
            .teamOneScore(request.teamOneScore())
            .teamTwoScore(request.teamTwoScore())
            .submittedBy(currentUser)
            .submittedAt(Instant.now())
            .build();

    try {
      resultRepository.saveAndFlush(result);
    } catch (DataIntegrityViolationException ex) {
      throw new ResultAlreadySubmittedException(gameNumber);
    }

    progressionService.applyGameResult(result);
    match = matchSupport.requireMatch(matchId, tournamentId);
    return matchSupport.toDetail(match);
  }

  private TournamentMatchDetailResponse handleExistingResult(
      TournamentGameResult existing,
      SubmitTournamentGameResultRequest request,
      Match match) {
    if (isExactMatch(existing, request)) {
      return matchSupport.toDetail(match);
    }
    throw new ResultAlreadySubmittedException(existing.getGameNumber());
  }

  private boolean isExactMatch(
      TournamentGameResult existing, SubmitTournamentGameResultRequest request) {
    return existing.getTeamOneScore() == request.teamOneScore()
        && existing.getTeamTwoScore() == request.teamTwoScore();
  }

  private void validateMatchReady(Match match) {
    if (match.isBye()) {
      throw new InvalidTournamentStateException("Cannot submit results for a bye match");
    }
    if (match.getStatus() == MatchStatus.PENDING) {
      throw new InvalidTournamentStateException("Match must be started before submitting results");
    }
    if (match.getTeamOne() == null || match.getTeamTwo() == null) {
      throw new InvalidTournamentStateException("Both teams must be assigned");
    }
  }
}
