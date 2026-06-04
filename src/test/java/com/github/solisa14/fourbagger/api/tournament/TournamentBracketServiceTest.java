package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TournamentBracketServiceTest {

  @Test
  void planBracket_whenSingleElimination_delegatesToSingleEliminationGenerator() {
    RecordingSingleEliminationGenerator singleGenerator = new RecordingSingleEliminationGenerator();
    TournamentBracketService service = new TournamentBracketService(singleGenerator);
    Tournament tournament = tournament(TournamentFormat.SINGLE_ELIMINATION);
    List<TournamentTeam> teams = List.of();

    service.planBracket(tournament, teams);

    assertThat(singleGenerator.callCount).isEqualTo(1);
    assertThat(singleGenerator.tournament).isSameAs(tournament);
    assertThat(singleGenerator.seededTeams).isSameAs(teams);
  }

  @Test
  void planBracket_whenFormatIsNull_defaultsToSingleEliminationGenerator() {
    RecordingSingleEliminationGenerator singleGenerator = new RecordingSingleEliminationGenerator();
    TournamentBracketService service = new TournamentBracketService(singleGenerator);
    Tournament tournament = tournament(null);
    List<TournamentTeam> teams = List.of();

    service.planBracket(tournament, teams);

    assertThat(singleGenerator.callCount).isEqualTo(1);
    assertThat(singleGenerator.tournament).isSameAs(tournament);
    assertThat(singleGenerator.seededTeams).isSameAs(teams);
  }

  @Test
  void planBracket_whenDoubleElimination_usesSingleEliminationCompatibilityGenerator() {
    RecordingSingleEliminationGenerator singleGenerator = new RecordingSingleEliminationGenerator();
    TournamentBracketService service = new TournamentBracketService(singleGenerator);
    Tournament tournament = tournament(TournamentFormat.DOUBLE_ELIMINATION);
    List<TournamentTeam> teams = List.of();

    service.planBracket(tournament, teams);

    assertThat(singleGenerator.callCount).isEqualTo(1);
    assertThat(singleGenerator.tournament).isSameAs(tournament);
    assertThat(singleGenerator.seededTeams).isSameAs(teams);
  }

  private Tournament tournament(TournamentFormat format) {
    return Tournament.builder().format(format).build();
  }

  private static class RecordingSingleEliminationGenerator extends SingleEliminationBracketGenerator {

    private int callCount;
    private Tournament tournament;
    private List<TournamentTeam> seededTeams;

    @Override
    public void planBracket(Tournament tournament, List<TournamentTeam> seededTeams) {
      this.callCount++;
      this.tournament = tournament;
      this.seededTeams = seededTeams;
    }
  }
}
