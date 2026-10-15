ALTER TABLE ${flyway:defaultSchema}.appointments ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);
