import { ClinicActivatedResponse } from '../../core/models/api.models';

export type ActivationState =
  | { status: 'loading' }
  | { status: 'success'; result: ClinicActivatedResponse }
  | { status: 'invalid' }
  | { status: 'error'; message: string };
