import { CdkTrapFocus } from '@angular/cdk/a11y';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, fromEvent } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { navItemsFor } from '../../core/navigation/navigation';
import { ROLE_LABEL } from '../../core/utils/labels.util';
import { GlassBackgroundComponent } from '../../shared/ui/glass-background/glass-background.component';
import { SidebarNavComponent } from './components/sidebar-nav/sidebar-nav.component';
import { TopbarComponent } from './components/topbar/topbar.component';

@Component({
  selector: 'app-shell',
  imports: [
    CdkTrapFocus,
    GlassBackgroundComponent,
    RouterOutlet,
    SidebarNavComponent,
    TopbarComponent,
  ],
  templateUrl: './shell.component.html',
  host: { '(document:keydown.escape)': 'closeNav()' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  private readonly auth = inject(AuthService);

  protected readonly navOpen = signal(false);
  private readonly drawerTrap = viewChild.required(CdkTrapFocus);
  private focusBeforeOpen: HTMLElement | null = null;

  protected readonly navItems = computed(() => navItemsFor(this.auth.role()));
  protected readonly userName = this.auth.displayName;
  protected readonly roleLabel = computed(() => {
    const role = this.auth.role();
    return role ? ROLE_LABEL[role] : '';
  });
  protected readonly clinicName = computed(() => this.auth.clinic()?.name ?? '');

  constructor() {
    inject(Router)
      .events.pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.navOpen.set(false));

    fromEvent<MediaQueryListEvent>(matchMedia('(min-width: 1024px)'), 'change')
      .pipe(
        filter((event) => event.matches),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.closeNav());
  }

  protected openNav(): void {
    this.focusBeforeOpen = document.activeElement as HTMLElement | null;
    this.navOpen.set(true);
    this.drawerTrap().focusTrap.focusFirstTabbableElementWhenReady().finally();
  }

  protected onNavClick(event: MouseEvent): void {
    if (event.target instanceof Element && event.target.closest('a')) this.closeNav();
  }

  protected closeNav(): void {
    if (!this.navOpen()) return;
    this.navOpen.set(false);
    this.focusBeforeOpen?.focus();
  }

  protected logout(): void {
    this.auth.logout();
  }
}
