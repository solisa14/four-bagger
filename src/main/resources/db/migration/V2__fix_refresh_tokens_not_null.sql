-- Remove any orphaned tokens first (safety)
DELETE FROM refresh_tokens WHERE user_id IS NULL;
ALTER TABLE refresh_tokens ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
