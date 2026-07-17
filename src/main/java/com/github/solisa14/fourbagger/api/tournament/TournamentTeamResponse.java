package com.github.solisa14.fourbagger.api.tournament;

import java.util.UUID;

/**
 * Draft or bracket team summary for tournament detail responses.
 *
 * @param id team id when persisted
 * @param playerOneParticipantId first member participant id
 * @param playerOneDisplayName first member display name (guests) or null
 * @param playerOneUsername first member username (accounts) or null
 * @param playerTwoParticipantId second member participant id, or null for singles
 * @param playerTwoDisplayName second member display name (guests) or null
 * @param playerTwoUsername second member username (accounts) or null
 * @param seed bracket seed when assigned; null during manual draft registration
 */
public record TournamentTeamResponse(
    UUID id,
    UUID playerOneParticipantId,
    String playerOneDisplayName,
    String playerOneUsername,
    UUID playerTwoParticipantId,
    String playerTwoDisplayName,
    String playerTwoUsername,
    Integer seed) {}
