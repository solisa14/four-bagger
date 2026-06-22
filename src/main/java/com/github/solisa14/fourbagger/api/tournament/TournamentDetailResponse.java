package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import java.util.List;
import java.util.UUID;

/**
 * Enriched tournament detail response including roster, eligibility, and viewer capabilities.
 */
public record TournamentDetailResponse(
    UUID id,
    String title,
    String joinCode,
    TournamentStatus status,
    GameType gameType,
    TournamentFormat format,
    TournamentBracketsResponse brackets,
    List<TournamentParticipantResponse> participants,
    TournamentBracketEligibilityResponse bracketEligibility,
    TournamentViewerCapabilitiesResponse viewerCapabilities) {}
