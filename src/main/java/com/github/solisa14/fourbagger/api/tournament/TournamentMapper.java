package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import org.springframework.stereotype.Component;

/** Mapper for tournament-related requests, commands, and responses. */
@Component
public class TournamentMapper {

  /**
   * Converts a tournament creation request to a creation command.
   *
   * @param organizer the user organizing the tournament
   * @param request the creation request
   * @return the creation command
   */
  public CreateTournamentCommand toCreateCommand(User organizer, CreateTournamentRequest request) {
    return new CreateTournamentCommand(organizer, request.title());
  }

  /**
   * Converts a tournament entity to a tournament response.
   *
   * @param tournament the tournament entity
   * @return the tournament response
   */
  public TournamentResponse toTournamentResponse(Tournament tournament) {
    return new TournamentResponse(
        tournament.getId(),
        tournament.getTitle(),
        tournament.getJoinCode(),
        tournament.getStatus());
  }

  /**
   * Converts a match entity to a match response.
   *
   * @param match the match entity
   * @return the match response
   */
  public MatchResponse toMatchResponse(Match match) {
    return new MatchResponse(
        match.getId(),
        match.getMatchNumber(),
        match.getStatus(),
        match.isBye(),
        match.getTeamOne() != null ? toTeamSummary(match.getTeamOne()) : null,
        match.getTeamTwo() != null ? toTeamSummary(match.getTeamTwo()) : null,
        match.getTeamOneWins(),
        match.getTeamTwoWins(),
        match.getWinner() != null ? toTeamSummary(match.getWinner()) : null);
  }

  /**
   * Converts a tournament team to a team summary DTO.
   *
   * @param team the tournament team
   * @return the team summary DTO
   */
  public MatchResponse.TeamSummary toTeamSummary(TournamentTeam team) {
    return new MatchResponse.TeamSummary(
        team.getId(),
        team.getPlayerOne().getUsername(),
        team.getPlayerTwo() != null ? team.getPlayerTwo().getUsername() : null,
        team.getSeed());
  }
}
