import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterRenderEffect,
  computed,
  inject,
  signal,
  viewChildren,
} from '@angular/core';
import { DialogRef } from '@angular/cdk/dialog';
import { FormsModule } from '@angular/forms';
import { initials } from '../../../../core/utils/labels.util';
import { specialtyLabel } from '../../../../shared/pipes/specialty-label.pipe';
import { DialogFrameComponent } from '../../../../shared/ui/dialog-frame/dialog-frame.component';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import { SelectComponent, SelectOption } from '../../../../shared/ui/forms/select/select.component';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';
import { PatientsService } from '../../services/patients.service';
import { SkeletonComponent } from '../../../../shared/ui/skeleton/skeleton.component';
import { SpecialtyLabelPipe } from '../../../../shared/pipes/specialty-label.pipe';

@Component({
  selector: 'app-assign-doctor-dialog',
  imports: [
    SpecialtyLabelPipe,
    SkeletonComponent,
    DialogFrameComponent,
    FormsModule,
    FormFieldComponent,
    SelectComponent,
    UiIconComponent,
  ],
  templateUrl: './assign-doctor-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignDoctorDialogComponent {
  protected readonly patients = inject(PatientsService);
  protected readonly ref = inject(DialogRef);

  protected readonly initials = initials;
  protected readonly doctorToAdd = signal<number | null>(null);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly removeButtons = viewChildren<ElementRef<HTMLButtonElement>>('removeButton');
  private readonly focusAfterChange = signal<(() => HTMLElement | null | undefined) | null>(null);
  protected readonly doctorOptions = computed<SelectOption<number>[]>(() =>
    this.patients.assignableDoctors().map((doctor) => ({
      value: doctor.id,
      label: `${doctor.name} · ${specialtyLabel(doctor.specialtyName)}`,
    })),
  );

  constructor() {
    afterRenderEffect(() => {
      const target = this.focusAfterChange();
      if (!target || this.patients.assigning()) return;
      if (this.patients.assignedDoctorsResource.isLoading()) return;
      target()?.focus();
      this.focusAfterChange.set(null);
    });
  }

  protected remove(doctorId: number, index: number): void {
    this.patients.unassignDoctor(doctorId);
    this.focusAfterChange.set(() => {
      const buttons = this.removeButtons();
      return (
        buttons[Math.min(index, buttons.length - 1)]?.nativeElement ??
        this.host.nativeElement.querySelector<HTMLElement>('h2')
      );
    });
  }

  protected add(): void {
    const doctorId = this.doctorToAdd();
    if (!doctorId) return;
    this.patients.assignDoctor(doctorId);
    this.doctorToAdd.set(null);
    this.focusAfterChange.set(() =>
      this.host.nativeElement.querySelector<HTMLElement>('#doctor-to-add'),
    );
  }
}
