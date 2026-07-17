package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TeamParticipantMigrationTest {

  @Test
  void v22MigrationRepointsTeamMembersAtParticipants() throws IOException {
    Path migration =
        Path.of(
            "src/main/resources/db/migration/V22__team_members_reference_participants.sql");
    String sql = Files.readString(migration);

    assertThat(sql).contains("ADD COLUMN player_one_participant_id");
    assertThat(sql).contains("ADD COLUMN player_two_participant_id");
    assertThat(sql).contains("DROP COLUMN player_one_id");
    assertThat(sql).contains("DROP COLUMN player_two_id");
    assertThat(sql).contains("REFERENCES tournament_participants (id)");
  }
}
