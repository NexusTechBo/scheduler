# Scheduler backend

## Test data (dev)

- `config/DataSeeder.java` seeds two tenants (clinics) only when the `clinic` table is empty. Tenant schemas are `clinic_<id>`.
- Login: `POST /api/auth/login`. Username = local part of the email + the clinic subdomain (e.g. `admin.downtown@clinic.com` in Downtown → `admin.downtown@downtownclinic.com`). Password for every seeded user: `password123`.
- Downtown (`@downtownclinic.com`): `admin.downtown` (ADMINISTRATOR), `ana.garcia` / `carlos.mendez` / `laura.torres` (DOCTOR), `maria.ramos` (ASSISTANT).
- Uptown (`@uptownclinic.com`, use it for tenant-isolation tests): `admin.uptown` (ADMINISTRATOR), `sofia.ramirez` / `diego.fernandez` (DOCTOR).
- Base URL: `http://localhost:${PORT}` (`PORT=8080` in `.env`).
