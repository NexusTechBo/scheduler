CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.specialties (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_${flyway:defaultSchema}_specialties_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.personal (
    id BIGSERIAL PRIMARY KEY,
    personal_account_id BIGINT NOT NULL REFERENCES public.accounts(id),
    specialty_id BIGINT REFERENCES ${flyway:defaultSchema}.specialties(id),
    role_id BIGINT NOT NULL REFERENCES public.roles(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_${flyway:defaultSchema}_personal_account UNIQUE (personal_account_id)
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.patients (
    id BIGSERIAL PRIMARY KEY,
    patient_account_id BIGINT NOT NULL REFERENCES public.accounts(id),
    role_id BIGINT NOT NULL REFERENCES public.roles(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_${flyway:defaultSchema}_patients_account UNIQUE (patient_account_id)
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.appointments (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.personal(id),
    patient_id BIGINT NOT NULL REFERENCES ${flyway:defaultSchema}.patients(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    version BIGINT,
    created_at TIMESTAMP NOT NULL
);

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

INSERT INTO ${flyway:defaultSchema}.specialties (id, name) VALUES (1, 'None') ON CONFLICT (id) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('${flyway:defaultSchema}.specialties', 'id'),
    (SELECT MAX(id) FROM ${flyway:defaultSchema}.specialties),
    true
);