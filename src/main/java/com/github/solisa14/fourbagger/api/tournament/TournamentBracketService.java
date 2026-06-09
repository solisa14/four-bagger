package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TournamentBracketService {

  private final TournamentBracketGenerator singleEliminationGenerator;
  private final TournamentBracketGenerator doubleEliminationGenerator;

  public TournamentBracketService(
      SingleEliminationBracketGenerator singleEliminationGenerator,
      DoubleEliminationBracketGenerator doubleEliminationGenerator) {
    this.singleEliminationGenerator = singleEliminationGenerator;
    this.doubleEliminationGenerator = doubleEliminationGenerator;
  }

  public void planBracket(Tournament tournament, List<TournamentTeam> seededTeams) {
    TournamentFormat format =
        tournament.getFormat() != null
            ? tournament.getFormat()
            : TournamentFormat.SINGLE_ELIMINATION;
    generatorFor(format).planBracket(tournament, seededTeams);
  }

  private TournamentBracketGenerator generatorFor(TournamentFormat format) {
    if (format == TournamentFormat.SINGLE_ELIMINATION) {
      return singleEliminationGenerator;
    }
    if (format == TournamentFormat.DOUBLE_ELIMINATION) {
      return doubleEliminationGenerator;
    }
    throw new InvalidTournamentStateException("Unsupported tournament format: " + format);
  }
}
