ALTER TABLE tournament_matches
    RENAME COLUMN next_match_id TO winner_next_match_id;

ALTER TABLE tournament_matches
    RENAME COLUMN next_match_position TO winner_next_match_position;

ALTER TABLE tournament_matches
    RENAME CONSTRAINT fk_tournament_matches_next_match TO fk_tournament_matches_winner_next_match;

ALTER INDEX idx_tournament_matches_next_match_id RENAME TO idx_tournament_matches_winner_next_match_id;

ALTER TABLE tournament_matches
    ADD COLUMN loser_next_match_id UUID,
    ADD COLUMN loser_next_match_position INT;

ALTER TABLE tournament_matches
    ADD CONSTRAINT fk_tournament_matches_loser_next_match
        FOREIGN KEY (loser_next_match_id) REFERENCES tournament_matches (id);

CREATE INDEX idx_tournament_matches_loser_next_match_id
    ON tournament_matches (loser_next_match_id);
