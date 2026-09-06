package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import java.util.List;

/** Identifier-free tournament data safe for the unauthenticated Shared Tournament View. */
public record SharedTournamentResponse(
        String title,
        TournamentStatus status,
        GameType gameType,
        TournamentFormat format,
        Brackets brackets) {

    public record Brackets(List<Round> winners, List<Round> losers, List<Round> finalRounds, List<Round> grandFinal) {}

    public record Round(BracketType bracketType, int roundNumber, int bestOf, List<Match> matches) {}

    public record Match(
            int matchNumber,
            MatchStatus status,
            boolean isBye,
            Team teamOne,
            Team teamTwo,
            int teamOneWins,
            int teamTwoWins,
            Team winner) {}

    public record Team(List<String> participantLabels, Integer seed, int losses, boolean eliminated) {}
}
