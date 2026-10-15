/**
 * Contracts of the scheduler backend (Spring Boot). Field names mirror the Java DTO records 1:1 —
 * when a DTO changes in backend/src/main/java/.../dto, update this file (see the `api-sync` skill).
 *
 * Wire formats: LocalDate = "yyyy-MM-dd", LocalTime = "HH:mm:ss", LocalDateTime = "yyyy-MM-ddTHH:mm:ss" (no zone).
 */

export type Role = 'ADMINISTRATOR' | 'DOCTOR' | 'ASSISTANT' | 'PATIENT';

/** Fixed ids seeded in public.roles (V1__init_public_schema.sql). */
export const ROLE_ID: Record<Role, number> = {
  ADMINISTRATOR: 1,
  DOCTOR: 2,
  ASSISTANT: 3,
  PATIENT: 4,
};

export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED';

export type DayOfWeek =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

/** Spring Data `Page` serialized in VIA_DTO mode. */
export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface PageQuery {
  page?: number;
  size?: number;
  /** e.g. "startTime,desc" */
  sort?: string;
}

export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  errors?: { field: string; message: string }[] | null;
}

// ── Auth ─────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
  clinicId: number;
}

export interface LoginResponse {
  token: string;
}

/** Claims written by JwtUtil.generate. `sub` is Personal.id for staff and Patient.id for patients. */
export interface TokenPayload {
  sub: string;
  role: Role;
  clinicId: number;
  username: string;
  iat: number;
  exp: number;
}

export interface ProfileResponse {
  id: number;
  ci: string;
  name: string;
  email: string;
  phoneNumber: string | null;
  role: Role;
  specialtyName: string | null;
}

// ── Clinics ──────────────────────────────────────────────────────────

export interface Clinic {
  id: number;
  name: string;
  phoneNumber: string;
}

export interface ClinicRequest {
  name: string;
  phoneNumber: string;
  adminName: string;
  adminEmail: string;
  adminPassword: string;
  adminCi: string;
}

export interface ClinicCreatedResponse {
  id: number;
  name: string;
  phoneNumber: string;
  adminPersonalId: number;
  adminEmail: string;
}

// ── Appointments ─────────────────────────────────────────────────────

export interface AppointmentRequest {
  doctorId: number;
  patientId: number;
  startTime: string;
}

export interface AppointmentResponse {
  id: number;
  startTime: string;
  endTime: string;
  doctorId: number;
  doctorName: string;
  doctorSpecialty: string | null;
  doctorEmail: string;
  patientId: number;
  patientName: string;
  patientEmail: string;
  status: AppointmentStatus;
  /** Optional message left by staff when cancelling; null otherwise. */
  cancellationReason: string | null;
  createdAt: string;
}

/** Item of GET /appointments/board and /appointments/calendar. */
export interface AppointmentSummaryItem {
  id: number;
  clientName: string;
  doctorName: string;
  /** "yyyy-MM-dd" */
  appointmentDate: string;
  /** "HH:mm:ss" */
  appointmentTime: string;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  doctorId: number;
  patientId: number;
}

export type AppointmentBoard = Record<AppointmentStatus, AppointmentSummaryItem[]>;

/** Keys are "MM-dd-yyyy". */
export type AppointmentCalendar = Record<string, AppointmentSummaryItem[]>;

export interface AppointmentFilters extends PageQuery {
  doctorId?: number;
  patientId?: number;
  status?: AppointmentStatus;
}

// ── Patients ─────────────────────────────────────────────────────────

export interface Patient {
  id: number;
  name: string;
  email: string;
  phoneNumber: string | null;
  active: boolean;
}

export interface PatientRegisterRequest {
  name: string;
  email: string;
  ci: string;
  password: string;
  phoneNumber?: string;
}

export interface PatientUpdateRequest {
  name: string;
  email: string;
  phoneNumber?: string | null;
}

// ── Staff (backend: "personal") ──────────────────────────────────────

export interface Staff {
  id: number;
  name: string;
  email: string;
  active: boolean;
  roleName: Role;
  specialtyName: string | null;
}

export interface StaffRegisterRequest {
  name: string;
  email: string;
  ci: string;
  password: string;
  roleId: number;
  /** Required for doctors, must be empty for assistants (SpecialtyRoleMatch). */
  specialtyId: number | null;
}

export interface StaffUpdateRequest {
  name: string;
  email: string;
  specialtyId?: number | null;
}

export interface StaffFilters extends PageQuery {
  specialtyId?: number;
  isActive?: boolean;
  roleId?: number;
}

export interface AssignPatientRequest {
  patientId: number;
  doctorId: number;
}

// ── Catalogs ─────────────────────────────────────────────────────────

export interface Specialty {
  id: number;
  name: string;
}

// ── Doctor agenda ────────────────────────────────────────────────────

export interface AvailabilityRequest {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
}

export interface Availability extends AvailabilityRequest {
  id: number;
  active: boolean;
}

export interface TimeSlot {
  startTime: string;
  endTime: string;
}

export interface AvailableSlots {
  date: string;
  doctorId: number;
  availableSlots: TimeSlot[];
}

export interface ScheduleExceptionRequest {
  date: string;
  startTime: string | null;
  endTime: string | null;
  isFullDayBlock: boolean;
  reason: string | null;
}

export interface ScheduleException extends ScheduleExceptionRequest {
  id: number;
}
