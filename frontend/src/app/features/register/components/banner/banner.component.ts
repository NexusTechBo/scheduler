import { NgOptimizedImage } from '@angular/common';
import { Component } from '@angular/core';
import { IconComponent } from '../../../../shared/ui/icon/icon.component';

@Component({
  selector: 'app-banner',
  imports: [NgOptimizedImage, IconComponent],
  templateUrl: './banner.component.html',
})
export class BannerComponent {}
