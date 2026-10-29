# Landing page screenshots — Scheduler (ClinicFlow)

- **Date:** 2026-10-28, clinic "Clínica San Rafael" (`sanrafael`), demo data
- **Capture:** Chrome via playwright-core, light theme, 2x pixel density (desktop 1440x900 renders as 2880x1800 PNG; mobile 390x844 renders as 780x1688)
- **Data change:** one appointment booked (Natalia Herrera with Dr. Andrés Villca, Thu 29 Oct 10:00–10:30, PENDING). Nothing else was created or edited. The staff dialog was cancelled, the register form was not submitted, and the patient dialog was closed without saving.

| File | Role | What it shows |
|------|------|---------------|
| `01-login.png` | public | Login page with the empty form and the blurred clinic hero image with the "Bienvenido" card |
| `02-register.png` | public | Clinic registration filled in for "Centro Médico Los Andes" (not submitted). The hint reads "terminarán en @losandes.com". Full page, 958 px tall |
| `10-assistant-dashboard.png` | Assistant | Home with KPI cards (12 today, 12 pending, 25 confirmed, 4 cancelled), today's agenda and quick links |
| `11-assistant-board.png` | Assistant | Kanban board for 28 Oct – 3 Nov with Pending / Confirmed / Cancelled columns |
| `12-assistant-calendar.png` | Assistant | October month view with appointment dots and counts, and the agenda panel for 28 Oct |
| `13-assistant-appointments.png` | Assistant | Appointments table with status badges, confirm/cancel actions and pagination (84 records) |
| `14-assistant-booking-ready.png` | Assistant | Booking flow just before confirming: Natalia Herrera, Dr. Andrés Villca, Thu 29 Oct, 10:00 slot selected, summary card |
| `15-assistant-booking-success.png` | Assistant | After booking: success toast "Cita agendada: queda pendiente de confirmación" over the calendar on 29 Oct (14 appointments) |
| `16-assistant-patients.png` | Assistant | Patient list with search, phone, status and actions (14 patients) |
| `17-assistant-patient-edit-dialog.png` | Assistant | "Editar paciente" dialog for Juan Carlos Mamani over the blurred list |
| `20-doctor-dashboard.png` | Doctor | Home for Dra. Lucía Fernández: KPIs, today's agenda (1 appointment) and quick links including "Mi disponibilidad" |
| `21-doctor-availability.png` | Doctor | Weekly schedule (Mon–Fri blocks, weekend "No atiende"), upcoming blocks (empty) and a preview of free slots |
| `22-doctor-board.png` | Doctor | Board showing only the doctor's own appointments |
| `23-doctor-calendar.png` | Doctor | October calendar as the doctor sees it, with the day agenda for 28 Oct |
| `30-admin-dashboard.png` | Administrator | Home with KPIs plus active doctors and assistants, agenda and quick links |
| `31-admin-staff.png` | Administrator | Staff list with role, specialty, status and the role filter tabs |
| `32-admin-new-staff-dialog.png` | Administrator | "Nuevo miembro del personal" filled in (Doctor, Dr. Ricardo Salinas, Cardiología, `rsalinas@sanrafael.com`), not saved |
| `33-admin-specialties.png` | Administrator | Specialty catalog with the "Nueva especialidad" side form |
| `34-admin-profile.png` | Administrator | "Mi perfil" with the identity card, personal data and password change form |
| `40-mobile-login.png` | public | Mobile login (390x844) |
| `41-mobile-assistant-dashboard.png` | Assistant | Mobile home with stacked KPI cards and the start of today's agenda |
| `42-mobile-assistant-board.png` | Assistant | Mobile board with the Pending column and the next column peeking in from the right |

## Visual notes

- `34-admin-profile.png`: the username `mquiroga@sanrafael.com` in the identity card wraps mid-word ("…sanrafael.co / m"). Worth fixing or cropping before using the shot.
- `21-doctor-availability.png`: the time ranges in the weekly blocks wrap onto two lines ("08:00 – / 12:00") at 1440 px. It looks a bit cramped but is acceptable.
- `33-admin-specialties.png`: "Medicina General" is truncated to "Medicina Gen…" in its chip, and the page is quite empty (5 specialties).
- `02-register.png`: the clinic name input clips "Centro Médico Los And…" because the input is half width.
- `20-doctor-dashboard.png` / `22-doctor-board.png`: there is not much data for this doctor today (1 appointment), so these look sparse compared with the assistant shots.
- `14-assistant-booking-ready.png`: the left column auto-scrolled, so step 1 (Paciente) is out of view. The patient is still shown in the summary card.
- `40-mobile-login.png`: on mobile the hero and ClinicFlow branding are hidden, so only the form card shows.
- No console errors and no failed requests during any capture.
