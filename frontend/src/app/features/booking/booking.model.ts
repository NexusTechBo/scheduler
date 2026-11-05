import { Patient, Staff } from '../../core/models/api.models';

export type DoctorOption = Pick<Staff, 'id' | 'name' | 'specialtyId' | 'specialtyName'>;

export interface BookingAttempt {
  doctor: DoctorOption;
  patient: Patient;
  date: string;
  startDateTime: string;
}
