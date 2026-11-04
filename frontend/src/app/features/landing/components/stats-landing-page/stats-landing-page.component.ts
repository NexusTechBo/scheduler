import { ChangeDetectionStrategy, Component } from '@angular/core';
import { LANDING_STATS } from '../../constants/landing.content';
import { RevealDirective } from '../../directives/reveal.directive';

@Component({
  selector: 'app-stats-landing-page',
  imports: [RevealDirective],
  templateUrl: './stats-landing-page.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatsLandingPageComponent {
  protected readonly stats = LANDING_STATS;
}
