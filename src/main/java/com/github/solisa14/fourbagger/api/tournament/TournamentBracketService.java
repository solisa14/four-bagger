package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TournamentBracketService {

  private final TournamentBracketGenerator singleEliminationGenerator;

  public TournamentBracketService(SingleEliminationBracketGenerator singleEliminationGenerator) {
    this.singleEliminationGenerator = singleEliminationGenerator;
  }

  public void planBracket(Tournament tournament, List<TournamentTeam> seededTeams) {
    TournamentFormat format =
        tournament.getFormat() != null
            ? tournament.getFormat()
            : TournamentFormat.SINGLE_ELIMINATION;
    generatorFor(format).planBracket(tournament, seededTeams);
  }

  private TournamentBracketGenerator generatorFor(TournamentFormat format) {
    // TODO: change this later to support actual doubleEliminationGenerator
    if (format == TournamentFormat.SINGLE_ELIMINATION
        || format == TournamentFormat.DOUBLE_ELIMINATION) {
      return singleEliminationGenerator;
    }
    throw new InvalidTournamentStateException("Unsupported tournament format: " + format);
  }
}
