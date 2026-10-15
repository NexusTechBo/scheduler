-- Duplicated active bookings (same doctor and start) already in the data would block the index:
-- keep the oldest one and cancel the rest.
UPDATE ${flyway:defaultSchema}.appointments a
SET status = 'CANCELLED'
WHERE a.status <> 'CANCELLED'
  AND EXISTS (
    SELECT 1 FROM ${flyway:defaultSchema}.appointments b
    WHERE b.doctor_id = a.doctor_id
      AND b.start_time = a.start_time
      AND b.status <> 'CANCELLED'
      AND b.id < a.id
  );

-- DB-level guard against concurrent double booking of the same slot.
CREATE UNIQUE INDEX IF NOT EXISTS uq_${flyway:defaultSchema}_appointments_doctor_start_active
    ON ${flyway:defaultSchema}.appointments (doctor_id, start_time)
    WHERE status <> 'CANCELLED';
