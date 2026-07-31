package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for tournament match start and read operations. */
@Service
public class TournamentMatchService {

  private final TournamentMatchSupport matchSupport;
  private final MatchRepository matchRepository;
  private final TournamentMatchAuthorizationService authorizationService;

  @Autowired
  TournamentMatchService(
      TournamentMatchSupport matchSupport,
      MatchRepository matchRepository,
      TournamentMatchAuthorizationService authorizationService) {
    this.matchSupport = matchSupport;
    this.matchRepository = matchRepository;
    this.authorizationService = authorizationService;
  }

  public TournamentMatchService(
      TournamentRepository tournamentRepository,
      MatchRepository matchRepository,
      TournamentMatchAuthorizationService authorizationService,
      TournamentMapper tournamentMapper,
      TournamentGameResultRepository resultRepository,
      TournamentProgressionService progressionService) {
    this(
        new TournamentMatchSupport(
            tournamentRepository,
            matchRepository,
            resultRepository,
            tournamentMapper,
            progressionService),
        matchRepository,
        authorizationService);
  }

  @Transactional
  public TournamentMatchDetailResponse startMatch(UUID tournamentId, UUID matchId, User currentUser) {
    Tournament tournament = matchSupport.requireTournament(tournamentId);
    Match match = matchSupport.requireMatch(matchId, tournamentId);
    authorizationService.authorizeMatchMutation(currentUser, tournament, match);

    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
      throw new InvalidTournamentStateException(
          "Cannot start a match unless the tournament is IN_PROGRESS");
    }
    validateStartable(match);

    if (match.getStatus() == MatchStatus.IN_PROGRESS && match.getStartedAt() != null) {
      return matchSupport.toDetail(match);
    }

    match.setStatus(MatchStatus.IN_PROGRESS);
    match.setStartedAt(Instant.now());
    match.setStartedBy(currentUser);
    matchRepository.save(match);
    return matchSupport.toDetail(match);
  }

  @Transactional(readOnly = true)
  public TournamentMatchDetailResponse getMatchDetail(
      UUID tournamentId, UUID matchId, User currentUser) {
    Tournament tournament = matchSupport.requireTournament(tournamentId);
    Match match = matchSupport.requireMatch(matchId, tournamentId);
    authorizationService.authorizeTournamentAccess(currentUser, tournament);
    return matchSupport.toDetail(match);
  }

  @Transactional(readOnly = true)
  public Match getMatch(UUID tournamentId, UUID matchId) {
    matchSupport.requireTournament(tournamentId);
    return matchSupport.requireMatch(matchId, tournamentId);
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
}
