package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Authorization checks for tournament access and match mutations. */
@Service
public class TournamentMatchAuthorizationService {

    public void authorizeTournamentAccess(User currentUser, Tournament tournament) {
        if (!canAccessTournament(currentUser, tournament)) {
            throw new TournamentAccessDeniedException(tournament.getId());
        }
    }

    public void authorizeMatchMutation(User currentUser, Tournament tournament, Match match) {
        if (!canMutateMatch(currentUser, tournament, match)) {
            throw new TournamentAccessDeniedException(tournament.getId());
        }
    }

    public void authorizeOrganizer(User currentUser, Tournament tournament) {
        if (!isOrganizer(currentUser, tournament)) {
            throw new TournamentAccessDeniedException(tournament.getId());
        }
    }

    public boolean canAccessTournament(User currentUser, Tournament tournament) {
        if (currentUser == null) {
            return false;
        }

        UUID currentUserId = currentUser.getId();
        if (isOrganizer(currentUser, tournament)) {
            return true;
        }

        if (tournament.getParticipationMode() == TournamentParticipationMode.ORGANIZER_MANAGED) {
            return false;
        }

        return tournament.getParticipants().stream().anyMatch(participant -> participant.matchesUser(currentUserId));
    }

    public boolean canMutateMatch(User currentUser, Tournament tournament, Match match) {
        if (currentUser == null) {
            return false;
        }
        if (isOrganizer(currentUser, tournament)) {
            return true;
        }
        return isMatchParticipant(currentUser, match);
    }

    private boolean isOrganizer(User currentUser, Tournament tournament) {
        return tournament.getOrganizer().getId().equals(currentUser.getId());
    }

    private boolean isMatchParticipant(User currentUser, Match match) {
        UUID userId = currentUser.getId();
        return isTeamMember(userId, match.getTeamOne()) || isTeamMember(userId, match.getTeamTwo());
    }

    private boolean isTeamMember(UUID userId, TournamentTeam team) {
        if (team == null) {
            return false;
        }
        if (team.getPlayerOne().matchesUser(userId)) {
            return true;
        }
        return team.getPlayerTwo() != null && team.getPlayerTwo().matchesUser(userId);
    }
}
