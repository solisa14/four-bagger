CREATE TABLE tournament_game_results
(
    id               UUID                     NOT NULL,
    match_id         UUID                     NOT NULL,
    game_number      INT                      NOT NULL,
    winner_team_id   UUID                     NOT NULL,
    team_one_score   INT                      NOT NULL,
    team_two_score   INT                      NOT NULL,
    submitted_by_id  UUID                     NOT NULL,
    submitted_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT                   NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tournament_game_results PRIMARY KEY (id),
    CONSTRAINT fk_tournament_game_results_match FOREIGN KEY (match_id) REFERENCES tournament_matches (id),
    CONSTRAINT fk_tournament_game_results_winner_team FOREIGN KEY (winner_team_id) REFERENCES tournament_teams (id),
    CONSTRAINT fk_tournament_game_results_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES users (id),
    CONSTRAINT uk_tournament_game_results_match_game_number UNIQUE (match_id, game_number),
    CONSTRAINT ck_tournament_game_results_team_one_score_nonnegative CHECK (team_one_score >= 0),
    CONSTRAINT ck_tournament_game_results_team_two_score_nonnegative CHECK (team_two_score >= 0),
    CONSTRAINT ck_tournament_game_results_scores_not_tied CHECK (team_one_score <> team_two_score)
);

CREATE INDEX idx_tournament_game_results_match_id ON tournament_game_results (match_id);
CREATE INDEX idx_tournament_game_results_winner_team_id ON tournament_game_results (winner_team_id);
CREATE INDEX idx_tournament_game_results_submitted_by_id ON tournament_game_results (submitted_by_id);
