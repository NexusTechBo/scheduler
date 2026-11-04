CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.specialties (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_${flyway:defaultSchema}_specialties_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.personal (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    ci VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    specialty_id BIGINT REFERENCES ${flyway:defaultSchema}.specialties(id),
    role VARCHAR(50) NOT NULL
        CHECK (role IN ('ADMINISTRATOR', 'DOCTOR', 'ASSISTANT')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    also_doctor BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_${flyway:defaultSchema}_personal_username UNIQUE (username),
    CONSTRAINT uq_${flyway:defaultSchema}_personal_ci UNIQUE (ci),
    CONSTRAINT uq_${flyway:defaultSchema}_personal_email UNIQUE (email),
    CONSTRAINT uq_${flyway:defaultSchema}_personal_phone_number UNIQUE (phone_number),
    CONSTRAINT ck_${flyway:defaultSchema}_personal_also_doctor_admin
        CHECK (NOT also_doctor OR role = 'ADMINISTRATOR'),
    CONSTRAINT ck_${flyway:defaultSchema}_personal_also_doctor_specialty
        CHECK (NOT also_doctor OR specialty_id IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_${flyway:defaultSchema}_personal_single_admin
    ON ${flyway:defaultSchema}.personal ((role))
    WHERE role = 'ADMINISTRATOR';

CREATE INDEX IF NOT EXISTS idx_${flyway:defaultSchema}_personal_specialty
    ON ${flyway:defaultSchema}.personal (specialty_id);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.patients (
    id BIGSERIAL PRIMARY KEY,
    ci VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_${flyway:defaultSchema}_patients_ci UNIQUE (ci),
    CONSTRAINT uq_${flyway:defaultSchema}_patients_email UNIQUE (email),
    CONSTRAINT uq_${flyway:defaultSchema}_patients_phone_number UNIQUE (phone_number)
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.appointments (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.personal(id),
    patient_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.patients(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    cancellation_reason VARCHAR(500),
    version BIGINT,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_${flyway:defaultSchema}_appointments_doctor_start_active
    ON ${flyway:defaultSchema}.appointments (doctor_id, start_time)
    WHERE status <> 'CANCELLED';

CREATE INDEX IF NOT EXISTS idx_${flyway:defaultSchema}_appointments_patient
    ON ${flyway:defaultSchema}.appointments (patient_id);

CREATE INDEX IF NOT EXISTS idx_${flyway:defaultSchema}_appointments_start_time
    ON ${flyway:defaultSchema}.appointments (start_time);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.doctor_availabilities (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.personal(id),
    day_of_week VARCHAR(50) NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    slot_duration_minutes INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_${flyway:defaultSchema}_doctor_availabilities_doctor_day
    ON ${flyway:defaultSchema}.doctor_availabilities (doctor_id, day_of_week);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.schedule_exceptions (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.personal(id),
    date DATE NOT NULL,
    start_time TIME WITHOUT TIME ZONE,
    end_time TIME WITHOUT TIME ZONE,
    is_full_day_block BOOLEAN NOT NULL,
    reason VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_${flyway:defaultSchema}_schedule_exceptions_doctor_date
    ON ${flyway:defaultSchema}.schedule_exceptions (doctor_id, date);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.doctor_patient (
    doctor_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.personal(id),
    patient_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.patients(id),
    PRIMARY KEY (doctor_id, patient_id)
);

CREATE INDEX IF NOT EXISTS idx_${flyway:defaultSchema}_doctor_patient_patient
    ON ${flyway:defaultSchema}.doctor_patient (patient_id);
