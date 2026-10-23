ALTER TABLE ${flyway:defaultSchema}.personal ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ${flyway:defaultSchema}.patients ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
