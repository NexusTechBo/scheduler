CREATE UNIQUE INDEX IF NOT EXISTS uq_${flyway:defaultSchema}_appointments_doctor_start_active
    ON ${flyway:defaultSchema}.appointments (doctor_id, start_time)
    WHERE status <> 'CANCELLED';
