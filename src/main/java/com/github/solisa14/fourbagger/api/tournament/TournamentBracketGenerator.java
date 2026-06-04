package com.github.solisa14.fourbagger.api.tournament;

import java.util.List;

public interface TournamentBracketGenerator {

  TournamentFormat format();

  void planBracket(Tournament tournament, List<TournamentTeam> seededTeams);
}
