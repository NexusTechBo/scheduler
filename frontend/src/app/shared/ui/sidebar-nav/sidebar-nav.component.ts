import { Component, computed, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NavItem } from '../../../core/navigation/navigation';
import { initials } from '../../../core/utils/labels.util';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-sidebar-nav',
  imports: [RouterLink, RouterLinkActive, UiIconComponent],
  templateUrl: './sidebar-nav.component.html',
  host: {
    class:
      'glass-panel z-40 m-4 flex w-72 shrink-0 flex-col rounded-3xl !border-white/70 transition-transform duration-300 max-lg:fixed max-lg:inset-y-0 max-lg:left-0 max-lg:max-w-[calc(100vw-2rem)] lg:relative lg:z-10',
  },
})
export class SidebarNavComponent {
  navItems = input.required<NavItem[]>();
  clinicName = input('Mi clínica');
  roleLabel = input('');

  logout = output<void>();

  protected readonly clinicInitials = computed(() => initials(this.clinicName()));
}
