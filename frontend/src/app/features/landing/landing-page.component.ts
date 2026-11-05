import { ChangeDetectionStrategy, Component, ViewEncapsulation } from '@angular/core';
import { CtaLandingPageComponent } from './components/cta-landing-page/cta-landing-page.component';
import { FeaturesLandingPageComponent } from './components/features-landing-page/features-landing-page.component';
import { FooterLandingPageComponent } from './components/footer-landing-page/footer-landing-page.component';
import { HeaderLandingPageComponent } from './components/header-landing-page/header-landing-page.component';
import { HeroLandingPageComponent } from './components/hero-landing-page/hero-landing-page.component';
import { RolesLandingPageComponent } from './components/roles-landing-page/roles-landing-page.component';
import { ShowcaseLandingPageComponent } from './components/showcase-landing-page/showcase-landing-page.component';
import { StatsLandingPageComponent } from './components/stats-landing-page/stats-landing-page.component';
import { StepsLandingPageComponent } from './components/steps-landing-page/steps-landing-page.component';

@Component({
  selector: 'app-landing-page',
  imports: [
    HeaderLandingPageComponent,
    HeroLandingPageComponent,
    StatsLandingPageComponent,
    FeaturesLandingPageComponent,
    ShowcaseLandingPageComponent,
    RolesLandingPageComponent,
    StepsLandingPageComponent,
    CtaLandingPageComponent,
    FooterLandingPageComponent,
  ],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.css',
  encapsulation: ViewEncapsulation.None,
  host: { class: 'block min-h-screen overflow-x-clip bg-neutral text-gray-900' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LandingPageComponent {}
