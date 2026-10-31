import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RevealDirective } from '../../directives/reveal.directive';
import { LANDING_ANCHORS, LANDING_STEPS } from '../../constants/landing.content';

@Component({
  selector: 'app-steps-landing-page',
  imports: [RevealDirective],
  templateUrl: './steps-landing-page.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StepsLandingPageComponent {
  protected readonly anchor = LANDING_ANCHORS.howItWorks;
  protected readonly steps = LANDING_STEPS;
}
