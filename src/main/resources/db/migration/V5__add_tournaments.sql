CREATE TABLE tournaments
(
    id           UUID                     NOT NULL,
    organizer_id UUID                     NOT NULL,
    status       VARCHAR(255)             NOT NULL,
    title        VARCHAR(255)             NOT NULL,
    join_code    VARCHAR(255)             NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tournaments PRIMARY KEY (id),
    CONSTRAINT fk_tournaments_organizer FOREIGN KEY (organizer_id) REFERENCES users (id),
    CONSTRAINT uk_tournaments_join_code UNIQUE (join_code)
);

CREATE TABLE tournament_teams
(
    id            UUID NOT NULL,
    tournament_id UUID NOT NULL,
    player_one_id UUID NOT NULL,
    player_two_id UUID,
    seed          INT,
    CONSTRAINT pk_tournament_teams PRIMARY KEY (id),
    CONSTRAINT fk_tournament_teams_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id),
    CONSTRAINT fk_tournament_teams_player_one FOREIGN KEY (player_one_id) REFERENCES users (id),
    CONSTRAINT fk_tournament_teams_player_two FOREIGN KEY (player_two_id) REFERENCES users (id)
);

CREATE INDEX idx_tournaments_organizer_id ON tournaments (organizer_id);
CREATE INDEX idx_tournament_teams_tournament_id ON tournament_teams (tournament_id);
CREATE INDEX idx_tournament_teams_player_one_id ON tournament_teams (player_one_id);
CREATE INDEX idx_tournament_teams_player_two_id ON tournament_teams (player_two_id);
