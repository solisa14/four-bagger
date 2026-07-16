-- Participation mode: existing tournaments are the self-join / join-code flow.
ALTER TABLE tournaments
    ADD COLUMN participation_mode VARCHAR(255);

UPDATE tournaments
SET participation_mode = 'SELF_JOIN'
WHERE participation_mode IS NULL;

ALTER TABLE tournaments
    ALTER COLUMN participation_mode SET NOT NULL;

-- Organizer-managed tournaments have no join code; uniqueness still applies when present.
ALTER TABLE tournaments
    ALTER COLUMN join_code DROP NOT NULL;

-- Guest participants: name-only, tournament-scoped; account participants keep user_id.
ALTER TABLE tournament_participants
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE tournament_participants
    ADD COLUMN display_name VARCHAR(255);

ALTER TABLE tournament_participants
    ADD CONSTRAINT chk_tournament_participants_identity
        CHECK (
            (user_id IS NOT NULL AND display_name IS NULL)
                OR (user_id IS NULL AND display_name IS NOT NULL)
            );

-- Normalized uniqueness for guest display names within a tournament.
CREATE UNIQUE INDEX uk_tournament_participants_tournament_display_name_normalized
    ON tournament_participants (tournament_id, lower(btrim(display_name)))
    WHERE display_name IS NOT NULL;

-- Account participants: unique (tournament_id, user_id) already exists; keep only when user present.
ALTER TABLE tournament_participants
    DROP CONSTRAINT uk_tournament_participants_tournament_user;

CREATE UNIQUE INDEX uk_tournament_participants_tournament_user
    ON tournament_participants (tournament_id, user_id)
    WHERE user_id IS NOT NULL;
