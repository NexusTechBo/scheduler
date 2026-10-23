import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-banner',
  imports: [NgOptimizedImage, UiIconComponent],
  templateUrl: './banner.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BannerComponent {}
