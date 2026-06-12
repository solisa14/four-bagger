package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Submits tournament physical game results and drives series progression. */
@Service
public class TournamentMatchResultService {

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;
  private final TournamentTeamRepository teamRepository;
  private final TournamentGameResultRepository resultRepository;
  private final TournamentMatchAuthorizationService authorizationService;
  private final FinalScoreValidator finalScoreValidator;
  private final TournamentProgressionService progressionService;
  private final TournamentMapper tournamentMapper;

  public TournamentMatchResultService(
      TournamentRepository tournamentRepository,
      MatchRepository matchRepository,
      TournamentTeamRepository teamRepository,
      TournamentGameResultRepository resultRepository,
      TournamentMatchAuthorizationService authorizationService,
      FinalScoreValidator finalScoreValidator,
      TournamentProgressionService progressionService,
      TournamentMapper tournamentMapper) {
    this.tournamentRepository = tournamentRepository;
    this.matchRepository = matchRepository;
    this.teamRepository = teamRepository;
    this.resultRepository = resultRepository;
    this.authorizationService = authorizationService;
    this.finalScoreValidator = finalScoreValidator;
    this.progressionService = progressionService;
    this.tournamentMapper = tournamentMapper;
  }

  @Transactional
  public TournamentMatchDetailResponse submitResult(
      UUID tournamentId,
      UUID matchId,
      int gameNumber,
      User currentUser,
      SubmitTournamentGameResultRequest request) {
    Tournament tournament = loadTournament(tournamentId);
    Match match = loadMatch(matchId, tournamentId);
    authorizationService.authorizeMatchMutation(currentUser, tournament, match);
    validateMatchReady(match);

    Optional<TournamentGameResult> existingResult = resultRepository.findByMatchIdAndGameNumber(matchId, gameNumber);
    if (existingResult.isPresent()) {
      return handleExistingResult(existingResult.get(), request, match);
    }

    Integer expectedGameNumber = progressionService.nextGameNumber(match);
    if (expectedGameNumber == null) {
      throw new InvalidTournamentStateException("Match series is already clinched");
    }
    if (gameNumber != expectedGameNumber) {
      throw new InvalidTournamentStateException(
          "Game number must be " + expectedGameNumber + ", got " + gameNumber);
    }

    TournamentTeam winnerTeam =
        teamRepository
            .findById(request.winnerTeamId())
            .orElseThrow(TournamentTeamNotFoundException::new);
    finalScoreValidator.validateScores(request.teamOneScore(), request.teamTwoScore());
    finalScoreValidator.validateWinnerHasHigherScore(
        request.winnerTeamId(),
        match.getTeamOne().getId(),
        match.getTeamTwo().getId(),
        request.teamOneScore(),
        request.teamTwoScore());

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
    match = loadMatch(matchId, tournamentId);
    return buildDetail(match);
  }

  @Transactional(readOnly = true)
  public TournamentMatchDetailResponse getMatchDetail(UUID tournamentId, UUID matchId) {
    loadTournament(tournamentId);
    Match match = loadMatch(matchId, tournamentId);
    return buildDetail(match);
  }

  private TournamentMatchDetailResponse handleExistingResult(
      TournamentGameResult existing,
      SubmitTournamentGameResultRequest request,
      Match match) {
    if (isExactMatch(existing, request)) {
      return buildDetail(match);
    }
    throw new ResultAlreadySubmittedException(existing.getGameNumber());
  }

  private boolean isExactMatch(
      TournamentGameResult existing, SubmitTournamentGameResultRequest request) {
    return existing.getWinnerTeam().getId().equals(request.winnerTeamId())
        && existing.getTeamOneScore() == request.teamOneScore()
        && existing.getTeamTwoScore() == request.teamTwoScore();
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

  private TournamentMatchDetailResponse buildDetail(Match match) {
    List<TournamentGameResult> results = resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId());
    return tournamentMapper.toMatchDetailResponse(
        match, results, progressionService.nextGameNumber(match));
  }
}
