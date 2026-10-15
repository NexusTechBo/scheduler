import { Component, computed, input, output } from '@angular/core';
import { IconComponent } from '../../../../shared/ui/icon/icon.component';
import { Clinic } from '../../../../core/models/api.models';

export type CardAccent = 'primary' | 'secondary' | 'tertiary';

const ACCENT_CLASSES: Record<CardAccent, { badge: string; icon: string }> = {
  primary: { badge: 'bg-primary/10', icon: 'text-primary' },
  secondary: { badge: 'bg-secondary/10', icon: 'text-secondary' },
  tertiary: { badge: 'bg-tertiary/10', icon: 'text-tertiary' },
};

@Component({
  selector: 'app-clinic-option-card',
  imports: [IconComponent],
  templateUrl: './clinic-option-card.component.html',
  host: { class: 'block h-full' },
})
export class ClinicOptionCardComponent {
  clinic = input.required<Clinic>();
  /** Marks the clinic used last time on this device. */
  highlighted = input(false);
  accent = input<CardAccent>('primary');

  select = output<Clinic>();

  protected readonly accentClasses = computed(() => ACCENT_CLASSES[this.accent()]);
}
