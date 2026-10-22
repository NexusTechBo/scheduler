import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatToday } from '../../../../core/utils/calendar.util';
import { firstName, initials } from '../../../../core/utils/labels.util';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink, UiIconComponent],
  templateUrl: './topbar.component.html',
  host: {
    class: 'flex items-center justify-between gap-3 px-4 py-4 sm:gap-6 sm:px-6 lg:px-8 lg:py-6',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopbarComponent {
  userName = input('');
  roleLabel = input('');
  menuOpen = input(false);

  menu = output<void>();

  protected readonly greeting = computed(() => {
    const hour = new Date().getHours();
    return hour < 12 ? 'Buenos días' : hour < 19 ? 'Buenas tardes' : 'Buenas noches';
  });
  protected readonly givenName = computed(() => firstName(this.userName()));
  protected readonly avatarInitials = computed(() => initials(this.userName()));
  protected readonly today = formatToday();
}
