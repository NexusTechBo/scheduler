import { Patient } from '../../core/models/api.models';

export type PatientScope = 'mine' | 'all';

export interface PatientRow {
  patient: Patient;
  mine: boolean;
}
