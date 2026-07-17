-- Organizer-managed doubles may use random or manual partner assignment.
-- Null for singles and self-join tournaments (always random pairing at bracket time).
ALTER TABLE tournaments
    ADD COLUMN doubles_pairing_mode VARCHAR(255);

-- Existing organizer-managed doubles default to random pairing.
UPDATE tournaments
SET doubles_pairing_mode = 'RANDOM'
WHERE participation_mode = 'ORGANIZER_MANAGED'
  AND game_type = 'DOUBLES'
  AND doubles_pairing_mode IS NULL;
