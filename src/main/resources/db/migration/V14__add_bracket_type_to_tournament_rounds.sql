ALTER TABLE tournament_rounds
    ADD COLUMN bracket_type VARCHAR(50) NOT NULL DEFAULT 'WINNERS';

ALTER TABLE tournament_rounds
    DROP CONSTRAINT uk_tournament_rounds_tournament_round_number;

ALTER TABLE tournament_rounds
    ADD CONSTRAINT uk_tournament_rounds_tournament_bracket_round_number
        UNIQUE (tournament_id, bracket_type, round_number);

ALTER TABLE tournament_rounds
    ALTER COLUMN bracket_type DROP DEFAULT;

ALTER TABLE tournament_teams
    ADD COLUMN losses INT NOT NULL DEFAULT 0,
    ADD COLUMN is_eliminated BOOLEAN NOT NULL DEFAULT FALSE;
