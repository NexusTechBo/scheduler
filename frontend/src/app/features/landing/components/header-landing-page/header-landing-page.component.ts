import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';
import { LANDING_NAV } from '../../constants/landing.content';
import { AuthService } from '../../../../core/auth/auth.service';
import { HOME_URL } from '../../../../core/navigation/navigation';
@Component({
  selector: 'app-header-landing-page',
  templateUrl: './header-landing-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiIconComponent, RouterLink],
})
export class HeaderLandingPageComponent {
  protected readonly nav = LANDING_NAV;
  protected readonly homeUrl = HOME_URL;
  private readonly auth = inject(AuthService);
  protected readonly isLoggedIn = computed(() => !!this.auth.session());
}
