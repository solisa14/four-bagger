ALTER TABLE games
    ADD COLUMN submitted_by_id UUID,
    ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE games
    ADD CONSTRAINT fk_games_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES users (id);

CREATE INDEX idx_games_submitted_by_id ON games (submitted_by_id);
CREATE INDEX idx_games_completed_at ON games (completed_at);

ALTER TABLE tournament_matches
    ADD COLUMN started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN started_by_id UUID,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tournament_matches
    ADD CONSTRAINT fk_tournament_matches_started_by FOREIGN KEY (started_by_id) REFERENCES users (id);

ALTER TABLE tournament_teams
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tournaments
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
