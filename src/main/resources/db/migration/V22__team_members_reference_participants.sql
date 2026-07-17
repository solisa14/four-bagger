-- Teams reference tournament participants (account or guest) instead of users directly.
ALTER TABLE tournament_teams
    ADD COLUMN player_one_participant_id UUID,
    ADD COLUMN player_two_participant_id UUID;

UPDATE tournament_teams tt
SET player_one_participant_id = tp.id
FROM tournament_participants tp
WHERE tp.tournament_id = tt.tournament_id
  AND tp.user_id = tt.player_one_id;

UPDATE tournament_teams tt
SET player_two_participant_id = tp.id
FROM tournament_participants tp
WHERE tt.player_two_id IS NOT NULL
  AND tp.tournament_id = tt.tournament_id
  AND tp.user_id = tt.player_two_id;

ALTER TABLE tournament_teams
    ALTER COLUMN player_one_participant_id SET NOT NULL;

ALTER TABLE tournament_teams
    DROP CONSTRAINT fk_tournament_teams_player_one,
    DROP CONSTRAINT fk_tournament_teams_player_two;

DROP INDEX IF EXISTS idx_tournament_teams_player_one_id;
DROP INDEX IF EXISTS idx_tournament_teams_player_two_id;

ALTER TABLE tournament_teams
    DROP COLUMN player_one_id,
    DROP COLUMN player_two_id;

ALTER TABLE tournament_teams
    ADD CONSTRAINT fk_tournament_teams_player_one_participant
        FOREIGN KEY (player_one_participant_id) REFERENCES tournament_participants (id),
    ADD CONSTRAINT fk_tournament_teams_player_two_participant
        FOREIGN KEY (player_two_participant_id) REFERENCES tournament_participants (id);

CREATE INDEX idx_tournament_teams_player_one_participant_id
    ON tournament_teams (player_one_participant_id);
CREATE INDEX idx_tournament_teams_player_two_participant_id
    ON tournament_teams (player_two_participant_id);
