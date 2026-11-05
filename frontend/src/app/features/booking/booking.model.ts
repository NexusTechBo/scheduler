import { Patient, Staff } from '../../core/models/api.models';

export type DoctorOption = Pick<Staff, 'id' | 'name' | 'specialtyId' | 'specialtyName'>;

export interface BookingAttempt {
  doctor: DoctorOption;
  patient: Patient;
  /** Selected day, `yyyy-MM-dd`. */
  date: string;
  /** Selected slot start, `yyyy-MM-ddTHH:mm:ss`. */
  startDateTime: string;
}
