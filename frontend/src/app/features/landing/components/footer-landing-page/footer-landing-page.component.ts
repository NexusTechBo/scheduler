import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-footer-landing-page',
  templateUrl: './footer-landing-page.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterLandingPageComponent {
  protected readonly year = new Date().getFullYear();
}
