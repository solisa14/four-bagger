DROP TABLE IF EXISTS frames;

ALTER TABLE games
    DROP COLUMN IF EXISTS target_score,
    DROP COLUMN IF EXISTS scoring_mode;

ALTER TABLE tournament_rounds
    DROP COLUMN IF EXISTS scoring_mode;
