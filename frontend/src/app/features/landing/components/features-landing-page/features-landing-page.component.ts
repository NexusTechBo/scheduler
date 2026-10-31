import { ChangeDetectionStrategy, Component } from '@angular/core';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';
import { RevealDirective } from '../../directives/reveal.directive';
import { LANDING_ANCHORS, LANDING_FEATURES } from '../../constants/landing.content';

@Component({
  selector: 'app-features-landing-page',
  imports: [UiIconComponent, RevealDirective],
  templateUrl: './features-landing-page.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeaturesLandingPageComponent {
  protected readonly anchor = LANDING_ANCHORS.features;
  protected readonly features = LANDING_FEATURES;
}
