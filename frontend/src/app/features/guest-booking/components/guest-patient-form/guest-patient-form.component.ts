import { ChangeDetectionStrategy, Component, OnInit, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PatientRequest } from '../../../../core/models/api.models';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import {
  CI_ERRORS,
  PHONE_ERRORS,
  ciValidator,
  notBlank,
  phoneValidator,
} from '../../../../shared/utils/validation-messages.util';

@Component({
  selector: 'app-guest-patient-form',
  imports: [FormFieldComponent, ReactiveFormsModule],
  templateUrl: './guest-patient-form.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuestPatientFormComponent implements OnInit {
  readonly initial = input<PatientRequest | null>(null);
  readonly submitted = output<PatientRequest>();
  readonly back = output<void>();

  protected readonly ciErrors = CI_ERRORS;
  protected readonly phoneErrors = PHONE_ERRORS;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    ci: ['', [notBlank, ciValidator]],
    name: ['', notBlank],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', phoneValidator],
  });

  ngOnInit(): void {
    const initial = this.initial();
    if (initial) this.form.setValue({ ...initial, phoneNumber: initial.phoneNumber ?? '' });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { ci, name, email, phoneNumber } = this.form.getRawValue();
    this.submitted.emit({
      ci: ci.trim(),
      name: name.trim(),
      email: email.trim(),
      phoneNumber: phoneNumber.trim() || null,
    });
  }
}
