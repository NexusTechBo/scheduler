import { ChangeDetectionStrategy, Component } from '@angular/core';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';
import { BrowserFrameComponent } from '../browser-frame/browser-frame.component';
import { RevealDirective } from '../../directives/reveal.directive';
import { LANDING_SHOWCASE } from '../../constants/landing.content';

@Component({
  selector: 'app-showcase-landing-page',
  imports: [UiIconComponent, BrowserFrameComponent, RevealDirective],
  templateUrl: './showcase-landing-page.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowcaseLandingPageComponent {
  protected readonly showcase = LANDING_SHOWCASE;
}
