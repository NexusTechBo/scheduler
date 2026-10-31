import { DialogRef } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  signal,
  viewChildren,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Patient } from '../../../../core/models/api.models';
import { initials, specialtyLabel } from '../../../../core/utils/labels.util';
import { SpecialtyLabelPipe } from '../../../../shared/pipes/specialty-label.pipe';
import { DialogFrameComponent } from '../../../../shared/ui/dialog-frame/dialog-frame.component';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import { SelectComponent, SelectOption } from '../../../../shared/ui/forms/select/select.component';
import { LoadErrorComponent } from '../../../../shared/ui/load-error/load-error.component';
import { SkeletonComponent } from '../../../../shared/ui/skeleton/skeleton.component';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';
import { focusAfterLoad } from '../../../../shared/utils/focus-after-load.util';
import { AssignDoctorService } from './assign-doctor.service';

export interface AssignDoctorData {
  patient: Patient;
}

@Component({
  selector: 'app-assign-doctor-dialog',
  imports: [
    SpecialtyLabelPipe,
    SkeletonComponent,
    LoadErrorComponent,
    DialogFrameComponent,
    FormsModule,
    FormFieldComponent,
    SelectComponent,
    UiIconComponent,
  ],
  providers: [AssignDoctorService],
  templateUrl: './assign-doctor-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignDoctorDialogComponent {
  protected readonly assignment = inject(AssignDoctorService);
  protected readonly ref = inject(DialogRef);

  protected readonly initials = initials;
  protected readonly doctorToAdd = signal<number | null>(null);
  private readonly removeButtons = viewChildren<ElementRef<HTMLButtonElement>>('removeButton');
  private readonly focusAfterChange = focusAfterLoad(
    () => this.assignment.assigning() || this.assignment.assignedLoading(),
  );
  protected readonly doctorOptions = computed<SelectOption<number>[]>(() =>
    this.assignment.assignableDoctors().map((doctor) => ({
      value: doctor.id,
      label: `${doctor.name} · ${specialtyLabel(doctor.specialtyName)}`,
    })),
  );

  protected remove(doctorId: number, index: number): void {
    this.assignment.unassign(doctorId);
    this.focusAfterChange(() => {
      const buttons = this.removeButtons();
      return buttons[Math.min(index, buttons.length - 1)]?.nativeElement;
    }, 'h2');
  }

  protected add(): void {
    const doctorId = this.doctorToAdd();
    if (!doctorId) return;
    this.assignment.assign(doctorId);
    this.doctorToAdd.set(null);
    this.focusAfterChange('#doctor-to-add');
  }
}
