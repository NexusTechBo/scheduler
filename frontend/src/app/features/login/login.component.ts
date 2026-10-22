import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BannerComponent } from '../../shared/ui/banner/banner.component';
import { GlassBackgroundComponent } from '../../shared/ui/glass-background/glass-background.component';
import { LoginFormComponent } from './components/login-form/login-form.component';
import { LoginService } from './services/login.service';

@Component({
  selector: 'app-login',
  imports: [BannerComponent, GlassBackgroundComponent, LoginFormComponent],
  providers: [LoginService],
  templateUrl: './login.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {}
