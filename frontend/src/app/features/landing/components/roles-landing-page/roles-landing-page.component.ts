import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';
import { BrowserFrameComponent } from '../browser-frame/browser-frame.component';
import { RevealDirective } from '../../directives/reveal.directive';
import { LANDING_ROLES } from '../../constants/landing.content';

@Component({
  selector: 'app-roles-landing-page',
  imports: [UiIconComponent, BrowserFrameComponent, RevealDirective],
  templateUrl: './roles-landing-page.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RolesLandingPageComponent {
  protected readonly roles = LANDING_ROLES;
  protected readonly activeRoleId = signal(LANDING_ROLES[0].id);
  protected readonly activeRole = computed(
    () => this.roles.find((role) => role.id === this.activeRoleId()) ?? this.roles[0],
  );
}
