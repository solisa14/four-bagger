package com.github.solisa14.fourbagger.api.tournament;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves and reverts one-team or empty matches in a double-elimination bracket once all feeder
 * matches are settled.
 */
@Component
class DoubleEliminationByeResolver {

    /**
     * Auto-completes PENDING matches that have at most one team once every feeder match is COMPLETED.
     *
     * @param matches all matches in the tournament bracket graph
     * @return matches mutated during the cascade, including downstream destinations
     */
    List<Match> autoAdvanceResolvedByes(List<Match> matches) {
        Set<Match> changed = new LinkedHashSet<>();

        boolean advanced;
        do {
            advanced = false;
            for (Match match : matches) {
                if (match.getStatus() != MatchStatus.PENDING || !allSourcesCompleted(matches, match)) {
                    continue;
                }

                TournamentTeam onlyTeam = onlyTeam(match);
                if (teamCount(match) > 1) {
                    continue;
                }

                match.setBye(true);
                match.setStatus(MatchStatus.COMPLETED);
                match.setWinner(onlyTeam);
                changed.add(match);
                if (onlyTeam != null) {
                    Match destination = match.getWinnerNextMatch();
                    assignTeam(onlyTeam, destination, match.getWinnerNextMatchPosition());
                    if (destination != null) {
                        changed.add(destination);
                    }
                }
                advanced = true;
            }
        } while (advanced);

        return List.copyOf(changed);
    }

    /**
     * Resets COMPLETED bye matches whose feeders are no longer all completed, cascading winner
     * removals downstream. Seeded first-round byes (no feeders) and still-valid resolved byes are
     * left untouched.
     *
     * @param matches all matches in the tournament bracket graph
     * @param revertingSource match whose completion is being undone; treated as incomplete while
     *     evaluating feeder readiness
     * @return matches mutated during the cascade
     */
    List<Match> revertUnresolvedByes(List<Match> matches, Match revertingSource) {
        Set<Match> changed = new LinkedHashSet<>();

        boolean reverted;
        do {
            reverted = false;
            for (Match match : matches) {
                if (!match.isBye() || match.getStatus() != MatchStatus.COMPLETED) {
                    continue;
                }

                List<Match> sources = sourcesOf(matches, match);
                if (sources.isEmpty() || allSourcesCompleted(matches, match, revertingSource)) {
                    continue;
                }

                TournamentTeam previousWinner = match.getWinner();
                Match destination = match.getWinnerNextMatch();
                Integer position = match.getWinnerNextMatchPosition();

                match.setBye(false);
                match.setStatus(MatchStatus.PENDING);
                match.setWinner(null);
                changed.add(match);

                if (previousWinner != null) {
                    removeTeamFromSlot(previousWinner, destination, position);
                    if (destination != null) {
                        changed.add(destination);
                    }
                }
                reverted = true;
            }
        } while (reverted);

        return List.copyOf(changed);
    }

    private boolean allSourcesCompleted(List<Match> matches, Match destination) {
        return allSourcesCompleted(matches, destination, null);
    }

    private boolean allSourcesCompleted(List<Match> matches, Match destination, Match excludingSource) {
        List<Match> sources = sourcesOf(matches, destination);
        return !sources.isEmpty()
                && sources.stream()
                        .allMatch(source ->
                                !isSameMatch(source, excludingSource) && source.getStatus() == MatchStatus.COMPLETED);
    }

    private List<Match> sourcesOf(List<Match> matches, Match destination) {
        return matches.stream().filter(source -> routesTo(source, destination)).toList();
    }

    private boolean routesTo(Match source, Match destination) {
        return isSameMatch(source.getWinnerNextMatch(), destination)
                || isSameMatch(source.getLoserNextMatch(), destination);
    }

    private boolean isSameMatch(Match left, Match right) {
        if (left == null || right == null) {
            return false;
        }
        if (left == right) {
            return true;
        }
        return left.getId() != null && left.getId().equals(right.getId());
    }

    private int teamCount(Match match) {
        int count = 0;
        if (match.getTeamOne() != null) {
            count++;
        }
        if (match.getTeamTwo() != null) {
            count++;
        }
        return count;
    }

    private TournamentTeam onlyTeam(Match match) {
        if (match.getTeamOne() != null && match.getTeamTwo() == null) {
            return match.getTeamOne();
        }
        if (match.getTeamTwo() != null && match.getTeamOne() == null) {
            return match.getTeamTwo();
        }
        return null;
    }

    private void assignTeam(TournamentTeam team, Match destination, Integer position) {
        if (destination == null || position == null) {
            return;
        }
        if (position == 1) {
            destination.setTeamOne(team);
        } else if (position == 2) {
            destination.setTeamTwo(team);
        }
    }

    private void removeTeamFromSlot(TournamentTeam team, Match destination, Integer position) {
        if (destination == null || team == null || position == null) {
            return;
        }
        if (position == 1 && destination.getTeamOne() != null && sameTeam(destination.getTeamOne(), team)) {
            destination.setTeamOne(null);
        } else if (position == 2 && destination.getTeamTwo() != null && sameTeam(destination.getTeamTwo(), team)) {
            destination.setTeamTwo(null);
        }
    }

    private boolean sameTeam(TournamentTeam left, TournamentTeam right) {
        if (left == right) {
            return true;
        }
        return left.getId() != null && left.getId().equals(right.getId());
    }
}
