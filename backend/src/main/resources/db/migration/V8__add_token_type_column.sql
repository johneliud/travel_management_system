-- Add type column to distinguish EMAIL_VERIFICATION from PASSWORD_CHANGE tokens
-- 
ALTER TABLE email_verification_tokens ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'EMAIL_VERIFICATION';

CREATE INDEX idx_evt_user_type_used ON email_verification_tokens (user_id, type, used);
