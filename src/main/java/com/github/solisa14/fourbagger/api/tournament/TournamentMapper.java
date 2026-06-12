package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** Mapper for tournament-related requests, commands, and responses. */
@Component
public class TournamentMapper {

  public CreateTournamentCommand toCreateCommand(User organizer, CreateTournamentRequest request) {
    return new CreateTournamentCommand(
        organizer, request.title(), request.gameType(), request.format());
  }

  public TournamentResponse toTournamentResponse(Tournament tournament) {
    TournamentBracketsResponse brackets = toBracketsResponse(tournament.getRounds());
    return new TournamentResponse(
        tournament.getId(),
        tournament.getTitle(),
        tournament.getJoinCode(),
        tournament.getStatus(),
        tournament.getGameType(),
        tournament.getFormat(),
        brackets);
  }

  private TournamentBracketsResponse toBracketsResponse(List<TournamentRound> rounds) {
    return new TournamentBracketsResponse(
        roundsForBracket(rounds, BracketType.WINNERS),
        roundsForBracket(rounds, BracketType.LOSERS),
        roundsForBracket(rounds, BracketType.FINAL),
        activeGrandFinalRounds(rounds));
  }

  private List<TournamentRoundResponse> activeGrandFinalRounds(List<TournamentRound> rounds) {
    return rounds.stream()
        .filter(round -> round.getBracketType() == BracketType.GRAND_FINAL)
        .filter(
            round ->
                round.getMatches().stream()
                    .anyMatch(match -> match.getTeamOne() != null && match.getTeamTwo() != null))
        .sorted(Comparator.comparingInt(TournamentRound::getRoundNumber))
        .map(this::toRoundResponse)
        .toList();
  }

  private List<TournamentRoundResponse> roundsForBracket(
      List<TournamentRound> rounds, BracketType bracketType) {
    return rounds.stream()
        .filter(round -> bracketType == round.getBracketType())
        .sorted(Comparator.comparingInt(TournamentRound::getRoundNumber))
        .map(this::toRoundResponse)
        .toList();
  }

  private TournamentRoundResponse toRoundResponse(TournamentRound round) {
    List<MatchResponse> matches = round.getMatches().stream().map(this::toMatchResponse).toList();
    return new TournamentRoundResponse(
        round.getBracketType(), round.getRoundNumber(), round.getBestOf(), matches);
  }

  public MatchResponse toMatchResponse(Match match) {
    boolean winnerRouteVisible = isRouteVisible(match.getWinnerNextMatch());
    boolean loserRouteVisible = isRouteVisible(match.getLoserNextMatch());
    return new MatchResponse(
        match.getId(),
        match.getMatchNumber(),
        match.getStatus(),
        match.isBye(),
        match.getTeamOne() != null ? toTeamSummary(match.getTeamOne()) : null,
        match.getTeamTwo() != null ? toTeamSummary(match.getTeamTwo()) : null,
        match.getTeamOneWins(),
        match.getTeamTwoWins(),
        match.getWinner() != null ? toTeamSummary(match.getWinner()) : null,
        winnerRouteVisible ? match.getWinnerNextMatch().getId() : null,
        winnerRouteVisible ? match.getWinnerNextMatchPosition() : null,
        loserRouteVisible ? match.getLoserNextMatch().getId() : null,
        loserRouteVisible ? match.getLoserNextMatchPosition() : null);
  }

  public TournamentMatchDetailResponse toMatchDetailResponse(
      Match match, List<TournamentGameResult> results, Integer nextGameNumber) {
    MatchResponse base = toMatchResponse(match);
    int bestOf = match.getRound().getBestOf();
    int winsToClinch = (bestOf / 2) + 1;
    return new TournamentMatchDetailResponse(
        base.id(),
        base.matchNumber(),
        base.status(),
        base.isBye(),
        base.teamOne(),
        base.teamTwo(),
        base.teamOneWins(),
        base.teamTwoWins(),
        base.winner(),
        base.winnerNextMatchId(),
        base.winnerNextMatchPosition(),
        base.loserNextMatchId(),
        base.loserNextMatchPosition(),
        match.getStartedAt(),
        match.getStartedBy() != null ? match.getStartedBy().getId() : null,
        bestOf,
        winsToClinch,
        nextGameNumber,
        results.stream().map(this::toResultResponse).toList());
  }

  public TournamentGameResultResponse toResultResponse(TournamentGameResult result) {
    return new TournamentGameResultResponse(
        result.getGameNumber(),
        result.getWinnerTeam().getId(),
        result.getTeamOneScore(),
        result.getTeamTwoScore(),
        result.getSubmittedBy().getId(),
        result.getSubmittedAt());
  }

  private boolean isRouteVisible(Match destination) {
    if (destination == null) {
      return false;
    }
    return destination.getRound().getBracketType() != BracketType.GRAND_FINAL
        || (destination.getTeamOne() != null && destination.getTeamTwo() != null);
  }

  public MatchResponse.TeamSummary toTeamSummary(TournamentTeam team) {
    return new MatchResponse.TeamSummary(
        team.getId(),
        team.getPlayerOne().getUsername(),
        team.getPlayerTwo() != null ? team.getPlayerTwo().getUsername() : null,
        team.getSeed(),
        team.getLosses(),
        team.isEliminated());
  }
}
