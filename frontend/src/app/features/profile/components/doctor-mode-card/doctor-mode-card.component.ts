import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { AlertComponent } from '../../../../shared/ui/alert/alert.component';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import { SelectComponent, SelectOption } from '../../../../shared/ui/forms/select/select.component';
import { LoadErrorComponent } from '../../../../shared/ui/load-error/load-error.component';
import { ProfileService } from '../../profile.service';

@Component({
  selector: 'app-doctor-mode-card',
  imports: [
    AlertComponent,
    FormFieldComponent,
    LoadErrorComponent,
    ReactiveFormsModule,
    SelectComponent,
  ],
  templateUrl: './doctor-mode-card.component.html',
  host: { class: 'glass-panel block min-w-0 rounded-3xl p-6' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DoctorModeCardComponent {
  protected readonly profile = inject(ProfileService);

  private readonly enabling = signal(false);
  protected readonly enabled = this.profile.doctorMode;
  protected readonly checked = computed(() => this.enabled() || this.enabling());
  protected readonly showForm = computed(() => this.enabling() && !this.enabled());
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = new FormGroup({
    specialtyId: new FormControl<number | null>(null, Validators.required),
  });
  protected readonly specialtyOptions = computed<SelectOption<number | null>[]>(() =>
    this.profile.specialties().map((s) => ({ value: s.id, label: s.name })),
  );

  protected toggle(): void {
    if (this.saving()) return;
    this.error.set(null);
    if (!this.enabled()) {
      this.enabling.update((value) => !value);
      return;
    }
    this.saving.set(true);
    this.profile
      .disableDoctorMode()
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe();
  }

  protected save(): void {
    if (this.saving()) return;
    const specialtyId = this.form.controls.specialtyId.value;
    if (specialtyId === null) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.profile
      .enableDoctorMode(specialtyId)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.enabling.set(false);
          this.form.reset();
        },
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'No se pudo activar el modo doctor')),
      });
  }
}
