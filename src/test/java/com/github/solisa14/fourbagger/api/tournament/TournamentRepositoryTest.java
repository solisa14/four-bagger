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

  private final TournamentBracketService tournamentBracketService =
      new TournamentBracketService(
          new SingleEliminationBracketGenerator(),
          new DoubleEliminationBracketGenerator(new DoubleEliminationByeResolver()));

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager entityManager;

  private User savedUser(String suffix) {
    return userRepository.saveAndFlush(
        TestDataFactory.user(
            null, "user" + suffix, "encoded", Role.USER));
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
  void findDetailById_fetchesParticipantUsersBeforeEntityManagerIsCleared() {
    User organizer = savedUser("detail-organizer");
    User player = savedUser("detail-player");
    Tournament tournament =
        saveTournament(
            organizer, "Detail Tournament", "DETAIL", TournamentStatus.REGISTRATION);
    tournament
        .getParticipants()
        .add(TournamentParticipant.builder().tournament(tournament).user(player).build());
    UUID tournamentId = tournamentRepository.saveAndFlush(tournament).getId();
    entityManager.clear();

    Tournament detail = tournamentRepository.findDetailById(tournamentId).orElseThrow();
    entityManager.clear();

    assertThat(detail.getOrganizer().getUsername()).isEqualTo(organizer.getUsername());
    assertThat(detail.getParticipants())
        .extracting(participant -> participant.getUser().getUsername())
        .containsExactly(player.getUsername());
  }

  @Test
  void findDetailByJoinCode_fetchesParticipantUsersBeforeEntityManagerIsCleared() {
    User organizer = savedUser("joincode-organizer");
    User player = savedUser("joincode-player");
    Tournament tournament =
        saveTournament(
            organizer, "Join Code Tournament", "JOIN01", TournamentStatus.REGISTRATION);
    tournament
        .getParticipants()
        .add(TournamentParticipant.builder().tournament(tournament).user(player).build());
    tournamentRepository.saveAndFlush(tournament);
    entityManager.clear();

    Tournament detail = tournamentRepository.findDetailByJoinCode("JOIN01").orElseThrow();
    entityManager.clear();

    assertThat(detail.getOrganizer().getUsername()).isEqualTo(organizer.getUsername());
    assertThat(detail.getParticipants())
        .extracting(participant -> participant.getUser().getUsername())
        .containsExactly(player.getUsername());
  }

  @Test
  void findByOrganizerIdAndStatusIn_whenActiveAndCompletedExist_returnsOnlyActiveHosted() {
    User organizer = savedUser("active-host");
    saveTournament(organizer, "Hosted Registration", "HOST01", TournamentStatus.REGISTRATION);
    saveTournament(organizer, "Hosted Completed", "HOST02", TournamentStatus.COMPLETED);
    entityManager.clear();

    List<Tournament> result =
        tournamentRepository.findByOrganizer_IdAndStatusInOrderByUpdatedAtDesc(
            organizer.getId(),
            List.of(
                TournamentStatus.REGISTRATION,
                TournamentStatus.BRACKET_READY,
                TournamentStatus.IN_PROGRESS));

    assertThat(result).extracting(Tournament::getTitle).containsExactly("Hosted Registration");
  }

  @Test
  void findParticipatingActiveTournaments_whenActiveAndCompletedExist_returnsOnlyActivePlaying() {
    User organizer = savedUser("playing-organizer");
    User player = savedUser("active-player");
    Tournament active =
        saveTournament(organizer, "Playing Registration", "PLAY01", TournamentStatus.REGISTRATION);
    Tournament completed =
        saveTournament(organizer, "Playing Completed", "PLAY02", TournamentStatus.COMPLETED);
    active
        .getParticipants()
        .add(TournamentParticipant.builder().tournament(active).user(player).build());
    completed
        .getParticipants()
        .add(TournamentParticipant.builder().tournament(completed).user(player).build());
    tournamentRepository.saveAndFlush(active);
    tournamentRepository.saveAndFlush(completed);
    entityManager.clear();

    List<Tournament> result =
        tournamentRepository.findParticipatingActiveTournaments(
            player.getId(),
            List.of(
                TournamentStatus.REGISTRATION,
                TournamentStatus.BRACKET_READY,
                TournamentStatus.IN_PROGRESS));

    assertThat(result).extracting(Tournament::getTitle).containsExactly("Playing Registration");
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
        .allSatisfy(
            match -> assertThat(match.getWinnerNextMatch().getId()).isEqualTo(finalMatch.getId()));
    assertThat(roundOneMatches)
        .extracting(Match::getWinnerNextMatchPosition)
        .containsExactly(1, 2);
    assertThat(finalMatch.getWinnerNextMatch()).isNull();
    assertThat(finalMatch.getWinnerNextMatchPosition()).isNull();
    assertThat(roundOneMatches).allSatisfy(match -> assertThat(match.getLoserNextMatch()).isNull());
    assertThat(roundOneMatches)
        .allSatisfy(match -> assertThat(match.getLoserNextMatchPosition()).isNull());
  }

  @Test
  void findForResponseById_whenMatchHasResetRoutes_canMapAfterEntityManagerIsCleared() {
    User organizer = savedUser("response-organizer");
    Tournament tournament =
        Tournament.builder()
            .organizer(organizer)
            .title("Response Graph Cup")
            .status(TournamentStatus.BRACKET_READY)
            .format(TournamentFormat.DOUBLE_ELIMINATION)
            .joinCode("RESP01")
            .build();
    List<TournamentTeam> teams = seededTeams(tournament, 4);
    tournament.getTeams().addAll(teams);

    tournamentBracketService.planBracket(tournament, teams);
    tournamentRepository.saveAndFlush(tournament);
    Match firstFinal =
        tournament.getRounds().stream()
            .filter(round -> round.getBracketType() == BracketType.FINAL)
            .flatMap(round -> round.getMatches().stream())
            .findFirst()
            .orElseThrow();
    UUID firstFinalId = firstFinal.getId();
    entityManager.clear();

    Match detachedMatch = matchRepository.findForResponseById(firstFinalId).orElseThrow();
    entityManager.clear();
    MatchResponse response =
        new TournamentMapper(new TournamentBracketEligibilityPolicy()).toMatchResponse(detachedMatch);

    assertThat(response.id()).isEqualTo(firstFinalId);
    assertThat(response.winnerNextMatchId()).isNull();
    assertThat(response.winnerNextMatchPosition()).isNull();
    assertThat(response.loserNextMatchId()).isNull();
    assertThat(response.loserNextMatchPosition()).isNull();
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

  private Tournament saveTournament(
      User organizer, String title, String joinCode, TournamentStatus status) {
    return tournamentRepository.saveAndFlush(
        Tournament.builder()
            .organizer(organizer)
            .title(title)
            .joinCode(joinCode)
            .status(status)
            .build());
  }
}
