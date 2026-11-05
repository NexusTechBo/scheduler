import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { firstName, initials } from '../../../../core/utils/labels.util';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink, UiIconComponent, TranslatePipe],
  templateUrl: './topbar.component.html',
  host: {
    class: 'flex items-center justify-between gap-3 px-4 py-4 sm:gap-6 sm:px-6 lg:px-8 lg:py-6',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopbarComponent {
  private readonly i18n = inject(I18nService);

  userName = input('');
  roleLabel = input('');
  menuOpen = input(false);

  menu = output<void>();

  protected readonly today = computed(() => {
    const lang = this.i18n.currentLang();
    const locale = lang === 'es' ? 'es-BO' : 'en-US';
    return new Intl.DateTimeFormat(locale, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    }).format(new Date());
  });

  protected readonly greeting = computed(() => {
    const hour = new Date().getHours();
    const key =
      hour < 12
        ? 'nav.greetings.morning'
        : hour < 19
          ? 'nav.greetings.afternoon'
          : 'nav.greetings.evening';
    return this.i18n.translate(key);
  });

  protected readonly givenName = computed(() => firstName(this.userName()));
  protected readonly avatarInitials = computed(() => initials(this.userName()));
}
