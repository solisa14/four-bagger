package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ParticipationModeMigrationTest {

  @Test
  void v21MigrationBackfillsExistingTournamentsAsSelfJoin() throws IOException {
    Path migration =
        Path.of("src/main/resources/db/migration/V21__add_participation_mode_and_guest_participants.sql");
    String sql = Files.readString(migration);

    assertThat(sql).contains("ADD COLUMN participation_mode");
    assertThat(sql).contains("SET participation_mode = 'SELF_JOIN'");
    assertThat(sql).contains("ALTER COLUMN join_code DROP NOT NULL");
    assertThat(sql).contains("ADD COLUMN display_name");
    assertThat(sql)
        .contains("uk_tournament_participants_tournament_display_name_normalized");
  }
}
