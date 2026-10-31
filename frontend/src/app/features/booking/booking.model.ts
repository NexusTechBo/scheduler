import { Staff } from '../../core/models/api.models';

export type DoctorOption = Pick<Staff, 'id' | 'name' | 'specialtyName'>;
