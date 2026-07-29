package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Shared tournament/match loading and match-detail assembly for match services. */
@Component
class TournamentMatchSupport {

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;
  private final TournamentGameResultRepository resultRepository;
  private final TournamentMapper tournamentMapper;
  private final TournamentProgressionService progressionService;

  TournamentMatchSupport(
      TournamentRepository tournamentRepository,
      MatchRepository matchRepository,
      TournamentGameResultRepository resultRepository,
      TournamentMapper tournamentMapper,
      TournamentProgressionService progressionService) {
    this.tournamentRepository = tournamentRepository;
    this.matchRepository = matchRepository;
    this.resultRepository = resultRepository;
    this.tournamentMapper = tournamentMapper;
    this.progressionService = progressionService;
  }

  Tournament requireTournament(UUID tournamentId) {
    return tournamentRepository
        .findById(tournamentId)
        .orElseThrow(TournamentNotFoundException::new);
  }

  Match requireMatch(UUID matchId, UUID tournamentId) {
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

  TournamentMatchDetailResponse toDetail(Match match) {
    List<TournamentGameResult> results =
        resultRepository.findByMatchIdOrderByGameNumberAsc(match.getId());
    return tournamentMapper.toMatchDetailResponse(
        match, results, progressionService.nextGameNumber(match));
  }
}
