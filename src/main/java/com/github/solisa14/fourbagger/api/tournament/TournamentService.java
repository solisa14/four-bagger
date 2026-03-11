package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.user.User;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {
  private static final SecureRandom RANDOM = new SecureRandom();
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

    TournamentTeam team = TournamentTeam.builder().tournament(tournament).playerOne(user).build();
    tournament.getTeams().add(team);
    tournamentRepository.save(tournament);
    return team;
  }

  public Tournament createTournament(User organizer, String title) {
    String joinCode = generateJoinCode();
    Tournament tournament =
        Tournament.builder()
            .organizer(organizer)
            .title(title)
            .status(TournamentStatus.REGISTRATION)
            .joinCode(joinCode)
            .build();
    return tournamentRepository.save(tournament);
  }

  private String generateJoinCode() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder sb = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
    }
    return sb.toString();
  }
}
