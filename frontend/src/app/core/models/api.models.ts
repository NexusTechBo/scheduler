export type Role = 'ADMINISTRATOR' | 'DOCTOR' | 'ASSISTANT';

export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED';

export type DayOfWeek =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

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
  sort?: string;
}

export interface ApiError {
  status: number;
  message: string;
  errors?: { field: string; message: string }[] | null;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface TokenPayload {
  sub: string;
  role: Role;
  clinicId: number;
  clinicName: string;
  clinicSubdomain: string;
  username: string;
  exp: number;
}

export interface ProfileUpdateRequest {
  name: string;
  email: string;
  phoneNumber?: string | null;
  currentPassword?: string | null;
  newPassword?: string | null;
}

export interface ClinicRequest {
  name: string;
  subdomain: string;
  phoneNumber: string;
  adminName: string;
  adminUsername: string;
  adminEmail: string;
  adminPassword: string;
  adminCi: string;
}

export interface ClinicCreatedResponse {
  id: number;
  name: string;
  phoneNumber: string;
  subdomain: string;
  adminUsername: string;
  adminEmail: string;
}

export interface ActivateClinicRequest {
  token: string;
}

export interface ClinicActivatedResponse {
  clinicName: string;
  adminUsername: string;
}

export interface ResendActivationRequest {
  email: string;
}

export interface AppointmentRequest {
  doctorId: number;
  patientId: number;
  startTime: string;
}

export interface CancelAppointmentRequest {
  reason: string;
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
  cancellationReason: string | null;
}

export interface AppointmentSummaryItem {
  id: number;
  clientName: string;
  doctorName: string;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  doctorId: number;
  patientId: number;
}

export type AppointmentBoard = Record<AppointmentStatus, AppointmentSummaryItem[]>;

export type AppointmentCalendar = Record<string, AppointmentSummaryItem[]>;

export interface AppointmentFilters extends PageQuery {
  doctorId?: number;
  patientId?: number;
  status?: AppointmentStatus;
}

export interface AppointmentBoardQuery {
  from: string;
  to: string;
  doctorId?: number;
  patientId?: number;
}

export interface AppointmentCalendarQuery {
  month: number;
  year: number;
  doctorId?: number;
  patientId?: number;
}

export interface Patient {
  id: number;
  ci: string;
  name: string;
  email: string;
  phoneNumber: string | null;
  active: boolean;
}

export interface PatientRequest {
  ci: string;
  name: string;
  email: string;
  phoneNumber?: string | null;
}

export interface Staff {
  id: number;
  username: string;
  ci: string;
  name: string;
  email: string;
  phoneNumber: string | null;
  active: boolean;
  role: Role;
  specialtyName: string | null;
}

export interface StaffRegisterRequest {
  name: string;
  email: string;
  phoneNumber?: string | null;
  ci: string;
  username: string;
  password: string;
  role: Role;
  specialtyId: number | null;
}

export interface StaffUpdateRequest {
  name: string;
  email: string;
  phoneNumber?: string | null;
  password?: string | null;
  role: Role;
  specialtyId: number | null;
}

export interface PatientFilters extends PageQuery {
  search?: string;
  isActive?: boolean;
}

export interface DoctorFilters extends PageQuery {
  specialtyId?: number;
  isActive?: boolean;
}

export interface StaffFilters extends PageQuery {
  specialtyId?: number;
  isActive?: boolean;
  role?: Role;
}

export interface AssignPatientRequest {
  patientId: number;
  doctorId: number;
}

export interface Specialty {
  id: number;
  name: string;
}

export interface SpecialtyRequest {
  name: string;
}

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
  cancelAppointments?: boolean;
}

export interface ScheduleException extends Omit<ScheduleExceptionRequest, 'cancelAppointments'> {
  id: number;
}
