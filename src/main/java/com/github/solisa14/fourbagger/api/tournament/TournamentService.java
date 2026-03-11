package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int MAX_JOIN_CODE_ATTEMPTS = 5;
  private final TournamentRepository tournamentRepository;

  public TournamentService(TournamentRepository tournamentRepository) {
    this.tournamentRepository = tournamentRepository;
  }

  public TournamentTeam joinTournament(String joinCode, User user) {
    Tournament tournament =
        tournamentRepository.findByJoinCode(joinCode).orElseThrow(TournamentNotFoundException::new);

    if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
      throw new InvalidTournamentStateException("Tournament is not open for registration");
    }

    boolean alreadyJoined =
        tournament.getTeams().stream()
            .anyMatch(
                team ->
                    user.getId().equals(team.getPlayerOne().getId())
                        || (team.getPlayerTwo() != null
                            && user.getId().equals(team.getPlayerTwo().getId())));
    if (alreadyJoined) {
      throw new DuplicateTournamentParticipantException();
    }

    TournamentTeam team = TournamentTeam.builder().tournament(tournament).playerOne(user).build();
    tournament.getTeams().add(team);
    tournamentRepository.save(tournament);
    return team;
  }

  public Tournament createTournament(User organizer, String title) {
    for (int attempt = 1; attempt <= MAX_JOIN_CODE_ATTEMPTS; attempt++) {
      String joinCode = generateJoinCode();
      Tournament tournament =
          Tournament.builder()
              .organizer(organizer)
              .title(title)
              .status(TournamentStatus.REGISTRATION)
              .joinCode(joinCode)
              .build();
      try {
        return tournamentRepository.save(tournament);
      } catch (DataIntegrityViolationException ex) {
        if (attempt == MAX_JOIN_CODE_ATTEMPTS) {
          throw new JoinCodeGenerationException();
        }
      }
    }
    throw new JoinCodeGenerationException();
  }

  public void deleteTournament(UUID id) {
    tournamentRepository.findById(id).orElseThrow(TournamentNotFoundException::new);
    tournamentRepository.deleteById(id);
  }

  private String generateJoinCode() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder sb = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
    }
    return sb.toString();
  }

  public void removeParticipant(UUID tournamentId, UUID participantId) {
    Tournament tournament =
        tournamentRepository.findById(tournamentId).orElseThrow(TournamentNotFoundException::new);

    if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
      throw new InvalidTournamentStateException("Cannot remove participants after registration");
    }

    boolean removed = tournament.getTeams().removeIf(team -> participantId.equals(team.getId()));
    if (!removed) {
      throw new TournamentTeamNotFoundException();
    }

    tournamentRepository.save(tournament);
  }
}
