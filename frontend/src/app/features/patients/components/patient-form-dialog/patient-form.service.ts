import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { PatientsApi } from '../../../../core/api/patients.api';
import { StaffApi } from '../../../../core/api/staff.api';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { Patient, PatientRequest } from '../../../../core/models/api.models';
import { NotificationService } from '../../../../shared/services/notification.service';

@Injectable({ providedIn: 'root' })
export class PatientFormService {
  private readonly patientsApi = inject(PatientsApi);
  private readonly staffApi = inject(StaffApi);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);

  save(patient: Patient | undefined, request: PatientRequest): Observable<Patient> {
    const save$ = patient
      ? this.patientsApi.update(patient.id, request)
      : this.patientsApi.create(request);
    return save$.pipe(
      switchMap((saved) =>
        !patient && this.auth.hasRole('DOCTOR')
          ? this.staffApi
              .assignPatient({ patientId: saved.id, doctorId: this.auth.userId()! })
              .pipe(
                map(() => saved),
                catchError((error) => {
                  this.notifications.error(
                    'Paciente creado, pero no se pudo asignar: ' + apiErrorMessage(error),
                  );
                  return of(saved);
                }),
              )
          : of(saved),
      ),
      tap(() => this.notifications.success(patient ? 'Paciente actualizado' : 'Paciente creado')),
    );
  }
}
