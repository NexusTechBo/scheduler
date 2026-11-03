ALTER TABLE ${flyway:defaultSchema}.personal
    ADD COLUMN IF NOT EXISTS also_doctor BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE ${flyway:defaultSchema}.personal
    ADD CONSTRAINT ck_${flyway:defaultSchema}_personal_also_doctor_admin
        CHECK (NOT also_doctor OR role = 'ADMINISTRATOR');

ALTER TABLE ${flyway:defaultSchema}.personal
    ADD CONSTRAINT ck_${flyway:defaultSchema}_personal_also_doctor_specialty
        CHECK (NOT also_doctor OR specialty_id IS NOT NULL);

CREATE UNIQUE INDEX IF NOT EXISTS uq_${flyway:defaultSchema}_personal_single_admin
    ON ${flyway:defaultSchema}.personal ((role))
    WHERE role = 'ADMINISTRATOR';
