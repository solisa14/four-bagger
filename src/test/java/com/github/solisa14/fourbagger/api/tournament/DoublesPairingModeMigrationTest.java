package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DoublesPairingModeMigrationTest {

  @Test
  void v23MigrationAddsDoublesPairingModeForOrganizerManagedDoubles() throws IOException {
    Path migration =
        Path.of("src/main/resources/db/migration/V23__add_doubles_pairing_mode.sql");
    String sql = Files.readString(migration);

    assertThat(sql).contains("ADD COLUMN doubles_pairing_mode");
    assertThat(sql).contains("ORGANIZER_MANAGED");
    assertThat(sql).contains("DOUBLES");
    assertThat(sql).contains("'RANDOM'");
  }
}
