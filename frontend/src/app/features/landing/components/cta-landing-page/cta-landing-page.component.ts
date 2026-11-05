import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';
import { RevealDirective } from '../../directives/reveal.directive';

@Component({
  selector: 'app-cta-landing-page',
  imports: [RouterLink, UiIconComponent, RevealDirective],
  templateUrl: './cta-landing-page.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CtaLandingPageComponent {}
