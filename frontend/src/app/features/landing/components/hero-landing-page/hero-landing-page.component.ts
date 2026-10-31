import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';
import { BrowserFrameComponent } from '../browser-frame/browser-frame.component';
import { RevealDirective } from '../../directives/reveal.directive';
import { LANDING_ANCHORS, HERO_MOBILE_SHOT, HERO_SHOT } from '../../constants/landing.content';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-hero-landing-page',
  imports: [RouterLink, UiIconComponent, BrowserFrameComponent, RevealDirective, NgOptimizedImage],
  templateUrl: './hero-landing-page.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeroLandingPageComponent {
  protected readonly productHref = '#' + LANDING_ANCHORS.product;
  protected readonly heroShot = HERO_SHOT;
  protected readonly heroMobileShot = HERO_MOBILE_SHOT;
}
