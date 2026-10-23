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
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_${flyway:defaultSchema}_personal_username UNIQUE (username),
    CONSTRAINT uq_${flyway:defaultSchema}_personal_ci UNIQUE (ci),
    CONSTRAINT uq_${flyway:defaultSchema}_personal_email UNIQUE (email),
    CONSTRAINT uq_${flyway:defaultSchema}_personal_phone_number UNIQUE (phone_number)
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.patients (
    id BIGSERIAL PRIMARY KEY,
    ci VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
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

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.doctor_availabilities (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.personal(id),
    day_of_week VARCHAR(50) NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    slot_duration_minutes INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.schedule_exceptions (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.personal(id),
    date DATE NOT NULL,
    start_time TIME WITHOUT TIME ZONE,
    end_time TIME WITHOUT TIME ZONE,
    is_full_day_block BOOLEAN NOT NULL,
    reason VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.doctor_patient (
    doctor_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.personal(id),
    patient_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.patients(id),
    PRIMARY KEY (doctor_id, patient_id)
);

