-- Existing refresh sessions become invalid after this migration.
DELETE FROM refresh_tokens;

DROP INDEX IF EXISTS idx_refresh_tokens_user;

ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS uk_refresh_tokens_token;
ALTER TABLE refresh_tokens DROP COLUMN token;

ALTER TABLE refresh_tokens ADD COLUMN token_hash VARCHAR(64) NOT NULL;

ALTER TABLE refresh_tokens ADD CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash);
ALTER TABLE refresh_tokens ADD CONSTRAINT uk_refresh_tokens_user UNIQUE (user_id);
