package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Mapper for tournament-related requests, commands, and responses. */
@Component
public class TournamentMapper {

  private final TournamentBracketEligibilityPolicy bracketEligibilityPolicy;

  public TournamentMapper(TournamentBracketEligibilityPolicy bracketEligibilityPolicy) {
    this.bracketEligibilityPolicy = bracketEligibilityPolicy;
  }

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

  public TournamentDetailResponse toTournamentDetailResponse(
      Tournament tournament, User currentViewer) {
    TournamentBracketsResponse brackets = toBracketsResponse(tournament.getRounds());
    BracketEligibility eligibility = bracketEligibilityPolicy.evaluate(tournament);
    boolean isOrganizer = isOrganizer(currentViewer, tournament);
    boolean isParticipant = isParticipant(currentViewer, tournament);
    return new TournamentDetailResponse(
        tournament.getId(),
        tournament.getTitle(),
        tournament.getJoinCode(),
        tournament.getStatus(),
        tournament.getGameType(),
        tournament.getFormat(),
        brackets,
        toParticipantResponses(tournament, currentViewer),
        toBracketEligibilityResponse(eligibility),
        toViewerCapabilitiesResponse(
            tournament, isOrganizer, isParticipant, eligibility.eligible()));
  }

  public TournamentListResponse toTournamentListResponse(ActiveTournaments tournaments) {
    return new TournamentListResponse(
        tournaments.hosting().stream().map(this::toTournamentSummaryResponse).toList(),
        tournaments.playing().stream().map(this::toTournamentSummaryResponse).toList());
  }

  public TournamentSummaryResponse toTournamentSummaryResponse(Tournament tournament) {
    return new TournamentSummaryResponse(
        tournament.getId(),
        tournament.getTitle(),
        tournament.getStatus(),
        tournament.getFormat(),
        tournament.getGameType());
  }

  private List<TournamentParticipantResponse> toParticipantResponses(
      Tournament tournament, User currentViewer) {
    UUID currentViewerId = currentViewer != null ? currentViewer.getId() : null;
    return tournament.getParticipants().stream()
        .sorted(
            Comparator.comparing(
                participant -> participant.getUser().getUsername(), String.CASE_INSENSITIVE_ORDER))
        .map(
            participant ->
                new TournamentParticipantResponse(
                    participant.getId(),
                    participant.getUser().getUsername(),
                    currentViewerId != null
                        && currentViewerId.equals(participant.getUser().getId())))
        .toList();
  }

  private TournamentBracketEligibilityResponse toBracketEligibilityResponse(
      BracketEligibility eligibility) {
    return new TournamentBracketEligibilityResponse(
        eligibility.eligible(),
        eligibility.participantCount(),
        eligibility.minimumParticipantCount(),
        eligibility.requiresEvenParticipantCount(),
        eligibility.message());
  }

  private TournamentViewerCapabilitiesResponse toViewerCapabilitiesResponse(
      Tournament tournament,
      boolean isOrganizer,
      boolean isParticipant,
      boolean bracketEligible) {
    boolean registration = tournament.getStatus() == TournamentStatus.REGISTRATION;
    return new TournamentViewerCapabilitiesResponse(
        isOrganizer,
        isOrganizer && registration && bracketEligible,
        isOrganizer && registration,
        isParticipant && registration);
  }

  private boolean isOrganizer(User currentViewer, Tournament tournament) {
    return currentViewer != null
        && tournament.getOrganizer().getId().equals(currentViewer.getId());
  }

  private boolean isParticipant(User currentViewer, Tournament tournament) {
    if (currentViewer == null) {
      return false;
    }
    UUID currentViewerId = currentViewer.getId();
    return tournament.getParticipants().stream()
        .anyMatch(participant -> participant.getUser().getId().equals(currentViewerId));
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
