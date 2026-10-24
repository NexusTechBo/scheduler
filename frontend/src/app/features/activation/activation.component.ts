import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  afterRenderEffect,
  inject,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ResendActivationComponent } from '../../shared/clinic-activation/components/resend-activation/resend-activation.component';
import { BannerComponent } from '../../shared/ui/banner/banner.component';
import { GlassBackgroundComponent } from '../../shared/ui/glass-background/glass-background.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { ActivationService } from './activation.service';

@Component({
  selector: 'app-activation',
  imports: [
    RouterLink,
    BannerComponent,
    GlassBackgroundComponent,
    ResendActivationComponent,
    UiIconComponent,
  ],
  providers: [ActivationService],
  templateUrl: './activation.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly activation = inject(ActivationService);

  private readonly token = this.route.snapshot.queryParamMap.get('token');
  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');

  constructor() {
    // Each state replaces the content (and the "Reintentar" button that held the focus), so
    // move the focus to the new heading whenever loading ends.
    afterRenderEffect(() => {
      if (this.activation.status() !== 'loading') this.heading()?.nativeElement.focus();
    });
  }

  ngOnInit(): void {
    // Drop the single-use token from the address bar and history before using it, so it
    // is neither kept in history nor leaked through the Referer header.
    if (this.token !== null) {
      this.router
        .navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true })
        .finally();
    }
    this.activation.activate(this.token);
  }
}
