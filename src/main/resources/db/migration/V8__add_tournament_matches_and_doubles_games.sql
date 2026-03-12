ALTER TABLE games
    ADD COLUMN game_type VARCHAR(50) NOT NULL DEFAULT 'SINGLES',
    ADD COLUMN scoring_mode VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN player_one_partner_id UUID,
    ADD COLUMN player_two_partner_id UUID;

ALTER TABLE games
    ADD CONSTRAINT fk_games_player_one_partner FOREIGN KEY (player_one_partner_id) REFERENCES users (id),
    ADD CONSTRAINT fk_games_player_two_partner FOREIGN KEY (player_two_partner_id) REFERENCES users (id);

CREATE INDEX idx_games_game_type ON games (game_type);
CREATE INDEX idx_games_scoring_mode ON games (scoring_mode);
CREATE INDEX idx_games_tournament_match_id ON games (tournament_match_id);

CREATE TABLE tournament_matches
(
    id                  UUID                     NOT NULL,
    round_id            UUID                     NOT NULL,
    team_one_id         UUID,
    team_two_id         UUID,
    match_number        INT                      NOT NULL,
    next_match_id       UUID,
    next_match_position INT,
    is_bye              BOOLEAN                  NOT NULL DEFAULT FALSE,
    status              VARCHAR(255)             NOT NULL DEFAULT 'PENDING',
    team_one_wins       INT                      NOT NULL DEFAULT 0,
    team_two_wins       INT                      NOT NULL DEFAULT 0,
    winner_id           UUID,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tournament_matches PRIMARY KEY (id),
    CONSTRAINT fk_tournament_matches_round FOREIGN KEY (round_id) REFERENCES tournament_rounds (id),
    CONSTRAINT fk_tournament_matches_team_one FOREIGN KEY (team_one_id) REFERENCES tournament_teams (id),
    CONSTRAINT fk_tournament_matches_team_two FOREIGN KEY (team_two_id) REFERENCES tournament_teams (id),
    CONSTRAINT fk_tournament_matches_next_match FOREIGN KEY (next_match_id) REFERENCES tournament_matches (id),
    CONSTRAINT fk_tournament_matches_winner FOREIGN KEY (winner_id) REFERENCES tournament_teams (id),
    CONSTRAINT uk_tournament_matches_round_match_number UNIQUE (round_id, match_number)
);

CREATE INDEX idx_tournament_matches_round_id ON tournament_matches (round_id);
CREATE INDEX idx_tournament_matches_next_match_id ON tournament_matches (next_match_id);
CREATE INDEX idx_tournament_matches_status ON tournament_matches (status);

ALTER TABLE games
    ADD CONSTRAINT fk_games_tournament_match FOREIGN KEY (tournament_match_id) REFERENCES tournament_matches (id);
