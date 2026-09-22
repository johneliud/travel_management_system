-- V7: Add optimistic locking version column to travels

ALTER TABLE travels ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
