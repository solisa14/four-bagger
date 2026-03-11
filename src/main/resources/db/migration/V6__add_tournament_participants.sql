CREATE TABLE tournament_participants
(
    id            UUID NOT NULL,
    tournament_id UUID NOT NULL,
    user_id       UUID NOT NULL,
    CONSTRAINT pk_tournament_participants PRIMARY KEY (id),
    CONSTRAINT fk_tournament_participants_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id),
    CONSTRAINT fk_tournament_participants_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_tournament_participants_tournament_user UNIQUE (tournament_id, user_id)
);

CREATE INDEX idx_tournament_participants_tournament_id ON tournament_participants (tournament_id);
CREATE INDEX idx_tournament_participants_user_id ON tournament_participants (user_id);
