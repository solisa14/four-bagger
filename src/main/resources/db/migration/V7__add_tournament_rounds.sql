CREATE TABLE tournament_rounds
(
    id            UUID                     NOT NULL,
    tournament_id UUID                     NOT NULL,
    round_number  INT                      NOT NULL,
    best_of       INT                      NOT NULL DEFAULT 1,
    scoring_mode  VARCHAR(255)             NOT NULL DEFAULT 'STANDARD',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tournament_rounds PRIMARY KEY (id),
    CONSTRAINT fk_tournament_rounds_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id),
    CONSTRAINT uk_tournament_rounds_tournament_round_number UNIQUE (tournament_id, round_number)
);

CREATE INDEX idx_tournament_rounds_tournament_id ON tournament_rounds (tournament_id);
