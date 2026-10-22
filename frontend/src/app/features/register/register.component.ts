import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BannerComponent } from '../../shared/ui/banner/banner.component';
import { GlassBackgroundComponent } from '../../shared/ui/glass-background/glass-background.component';
import { RegisterFormComponent } from './components/register-form/register-form.component';
import { RegisterService } from './services/register.service';

@Component({
  selector: 'app-register',
  imports: [RegisterFormComponent, BannerComponent, GlassBackgroundComponent],
  providers: [RegisterService],
  templateUrl: './register.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {}
