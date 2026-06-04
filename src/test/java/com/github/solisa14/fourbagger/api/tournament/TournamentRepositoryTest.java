package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.testsupport.AbstractDataJpaTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TournamentRepositoryTest extends AbstractDataJpaTest {

  private final TournamentBracketService tournamentBracketService = new TournamentBracketService();

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager entityManager;

  private User savedUser(String suffix) {
    return userRepository.saveAndFlush(
        TestDataFactory.user(
            null, "user" + suffix, "user" + suffix + "@example.com", "encoded", Role.USER));
  }

  @Test
  void findByJoinCode_whenCodeExists_returnsTournament() {
    User organizer = savedUser("a");
    tournamentRepository.saveAndFlush(
        TestDataFactory.tournament(organizer, "Summer Cup", "ABC123"));

    Optional<Tournament> result = tournamentRepository.findByJoinCode("ABC123");

    assertThat(result).isPresent();
    assertThat(result.get().getTitle()).isEqualTo("Summer Cup");
  }

  @Test
  void findByJoinCode_whenCodeDoesNotExist_returnsEmpty() {
    Optional<Tournament> result = tournamentRepository.findByJoinCode("NOTEXIST");

    assertThat(result).isEmpty();
  }

  @Test
  void findByTournament_whenBracketGenerated_persistsWinnerRoutingEdges() {
    User organizer = savedUser("organizer");
    Tournament tournament =
        Tournament.builder()
            .organizer(organizer)
            .title("Routing Cup")
            .status(TournamentStatus.BRACKET_READY)
            .joinCode("ROUTE1")
            .build();
    List<TournamentTeam> teams = seededTeams(tournament, 4);
    tournament.getTeams().addAll(teams);

    tournamentBracketService.planBracket(tournament, teams);
    UUID tournamentId = tournamentRepository.saveAndFlush(tournament).getId();
    entityManager.clear();

    List<Match> matches =
        matchRepository.findByRound_Tournament_IdOrderByRound_RoundNumberAscMatchNumberAsc(
            tournamentId);
    List<Match> roundOneMatches =
        matches.stream().filter(match -> match.getRound().getRoundNumber() == 1).toList();
    Match finalMatch =
        matches.stream()
            .filter(match -> match.getRound().getRoundNumber() == 2)
            .findFirst()
            .orElseThrow();

    assertThat(roundOneMatches).hasSize(2);
    assertThat(roundOneMatches)
        .allSatisfy(match -> assertThat(match.getNextMatch().getId()).isEqualTo(finalMatch.getId()));
    assertThat(roundOneMatches).extracting(Match::getNextMatchPosition).containsExactly(1, 2);
    assertThat(finalMatch.getNextMatch()).isNull();
    assertThat(finalMatch.getNextMatchPosition()).isNull();
  }

  private List<TournamentTeam> seededTeams(Tournament tournament, int count) {
    return java.util.stream.IntStream.rangeClosed(1, count)
        .mapToObj(
            seed ->
                TournamentTeam.builder()
                    .tournament(tournament)
                    .playerOne(savedUser("seed" + seed))
                    .seed(seed)
                    .build())
        .toList();
  }
}
