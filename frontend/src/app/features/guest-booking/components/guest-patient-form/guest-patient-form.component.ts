import { ChangeDetectionStrategy, Component, OnInit, inject, input, output } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { GuestBookingPatient } from '../../../../core/models/api.models';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import {
  CI_ERRORS,
  PHONE_ERRORS,
  ciValidator,
  notBlank,
  phoneValidator,
} from '../../../../shared/utils/validation-messages.util';

type PatientKind = 'returning' | 'new';

@Component({
  selector: 'app-guest-patient-form',
  imports: [FormFieldComponent, ReactiveFormsModule],
  templateUrl: './guest-patient-form.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuestPatientFormComponent implements OnInit {
  readonly initial = input<GuestBookingPatient | null>(null);
  readonly submitted = output<GuestBookingPatient>();
  readonly back = output<void>();

  protected readonly ciErrors = CI_ERRORS;
  protected readonly phoneErrors = PHONE_ERRORS;
  protected readonly kinds: { id: PatientKind; label: string }[] = [
    { id: 'returning', label: 'Ya me atendí en esta clínica' },
    { id: 'new', label: 'Es mi primera vez' },
  ];

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.group({
    kind: this.fb.control<PatientKind | null>(null, Validators.required),
    ci: this.fb.nonNullable.control('', [notBlank, ciValidator]),
    phoneNumber: this.fb.nonNullable.control('', [notBlank, phoneValidator]),
    name: this.fb.nonNullable.control({ value: '', disabled: true }, notBlank),
    email: this.fb.nonNullable.control({ value: '', disabled: true }, Validators.email),
  });
  protected readonly kind = toSignal(this.form.controls.kind.valueChanges, { initialValue: null });

  constructor() {
    this.form.controls.kind.valueChanges.pipe(takeUntilDestroyed()).subscribe((kind) => {
      const { name, email } = this.form.controls;
      for (const control of [name, email]) {
        if (kind === 'new') control.enable();
        else control.disable();
      }
    });
  }

  ngOnInit(): void {
    const initial = this.initial();
    if (!initial) return;
    this.form.patchValue({
      kind: initial.name ? 'new' : 'returning',
      ci: initial.ci,
      phoneNumber: initial.phoneNumber,
      name: initial.name ?? '',
      email: initial.email ?? '',
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const ci = this.form.controls.ci.value.trim();
    const phoneNumber = this.form.controls.phoneNumber.value.trim();
    if (this.kind() === 'returning') {
      this.submitted.emit({ ci, phoneNumber });
      return;
    }
    this.submitted.emit({
      ci,
      phoneNumber,
      name: this.form.controls.name.value.trim(),
      email: this.form.controls.email.value.trim() || null,
    });
  }
}
