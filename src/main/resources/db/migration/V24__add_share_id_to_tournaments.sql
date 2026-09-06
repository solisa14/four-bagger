ALTER TABLE tournaments
    ADD COLUMN share_id UUID;

CREATE UNIQUE INDEX ux_tournaments_share_id ON tournaments (share_id);
