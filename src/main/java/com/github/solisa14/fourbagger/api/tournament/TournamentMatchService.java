package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for tournament match start and read operations. */
@Service
public class TournamentMatchService {

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;
  private final TournamentMatchAuthorizationService authorizationService;
  private final TournamentMapper tournamentMapper;
  private final TournamentGameResultRepository resultRepository;
  private final TournamentProgressionService progressionService;

  public TournamentMatchService(
      TournamentRepository tournamentRepository,
      MatchRepository matchRepository,
      TournamentMatchAuthorizationService authorizationService,
      TournamentMapper tournamentMapper,
      TournamentGameResultRepository resultRepository,
      TournamentProgressionService progressionService) {
    this.tournamentRepository = tournamentRepository;
    this.matchRepository = matchRepository;
    this.authorizationService = authorizationService;
    this.tournamentMapper = tournamentMapper;
    this.resultRepository = resultRepository;
    this.progressionService = progressionService;
  }

  @Transactional
  public TournamentMatchDetailResponse startMatch(UUID tournamentId, UUID matchId, User currentUser) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    Match match = loadMatch(matchId, tournamentId);
    authorizationService.authorizeMatchMutation(currentUser, tournament, match);

    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
      throw new InvalidTournamentStateException(
          "Cannot start a match unless the tournament is IN_PROGRESS");
    }
    validateStartable(match);

    if (match.getStatus() == MatchStatus.IN_PROGRESS && match.getStartedAt() != null) {
      return buildDetail(match);
    }

    match.setStatus(MatchStatus.IN_PROGRESS);
    match.setStartedAt(Instant.now());
    match.setStartedBy(currentUser);
    matchRepository.save(match);
    return buildDetail(match);
  }

  @Transactional(readOnly = true)
  public TournamentMatchDetailResponse getMatchDetail(UUID tournamentId, UUID matchId) {
    tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    Match match = loadMatch(matchId, tournamentId);
    return buildDetail(match);
  }

  @Transactional(readOnly = true)
  public Match getMatch(UUID tournamentId, UUID matchId) {
    tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);
    return loadMatch(matchId, tournamentId);
  }

  private Match loadMatch(UUID matchId, UUID tournamentId) {
    Match match =
        matchRepository
            .findForResponseById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));
    validateMatchBelongsToTournament(tournamentId, match);
    return match;
  }

  private void validateStartable(Match match) {
    if (match.isBye()) {
      throw new InvalidTournamentStateException("Cannot start a bye match");
    }
    if (match.getStatus() == MatchStatus.COMPLETED) {
      throw new InvalidTournamentStateException("Cannot start a completed match");
    }
    if (match.getTeamOne() == null || match.getTeamTwo() == null) {
      throw new InvalidTournamentStateException("Cannot start a match until both teams are assigned");
    }
  }

  private void validateMatchBelongsToTournament(UUID tournamentId, Match match) {
    UUID ownerTournamentId = match.getRound().getTournament().getId();
    if (!tournamentId.equals(ownerTournamentId)) {
      throw new InvalidTournamentStateException("Match does not belong to this tournament");
    }
  }

  private TournamentMatchDetailResponse buildDetail(Match match) {
    List<TournamentGameResult> results =
        resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId());
    return tournamentMapper.toMatchDetailResponse(
        match, results, progressionService.nextGameNumber(match));
  }
}
