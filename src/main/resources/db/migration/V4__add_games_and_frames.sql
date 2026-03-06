CREATE TABLE games (
    id                  UUID            NOT NULL,
    player_one_id       UUID            NOT NULL,
    player_two_id       UUID            NOT NULL,
    player_one_score    INT             NOT NULL DEFAULT 0,
    player_two_score    INT             NOT NULL DEFAULT 0,
    target_score        INT             NOT NULL DEFAULT 21,
    win_by_two          BOOLEAN         NOT NULL DEFAULT FALSE,
    status              VARCHAR(50)     NOT NULL,
    winner_id           UUID,
    created_by_id       UUID            NOT NULL,
    tournament_match_id UUID,
    created_at          TIMESTAMP WITH TIME ZONE,
    updated_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_games PRIMARY KEY (id),
    CONSTRAINT fk_games_player_one FOREIGN KEY (player_one_id) REFERENCES users (id),
    CONSTRAINT fk_games_player_two FOREIGN KEY (player_two_id) REFERENCES users (id),
    CONSTRAINT fk_games_winner FOREIGN KEY (winner_id) REFERENCES users (id),
    CONSTRAINT fk_games_created_by FOREIGN KEY (created_by_id) REFERENCES users (id)
);
-- NOTE: tournament_match_id FK added in V5 after tournament_matches table exists

CREATE TABLE frames (
    id                        UUID    NOT NULL,
    game_id                   UUID    NOT NULL,
    frame_number              INT     NOT NULL,
    player_one_bags_in        INT     NOT NULL DEFAULT 0,
    player_one_bags_on        INT     NOT NULL DEFAULT 0,
    player_two_bags_in        INT     NOT NULL DEFAULT 0,
    player_two_bags_on        INT     NOT NULL DEFAULT 0,
    player_one_frame_points   INT     NOT NULL DEFAULT 0,
    player_two_frame_points   INT     NOT NULL DEFAULT 0,
    created_at                TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_frames PRIMARY KEY (id),
    CONSTRAINT fk_frames_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT uk_frames_game_number UNIQUE (game_id, frame_number)
);

CREATE INDEX idx_games_player_one ON games (player_one_id);
CREATE INDEX idx_games_player_two ON games (player_two_id);
CREATE INDEX idx_games_status ON games (status);
CREATE INDEX idx_frames_game ON frames (game_id);