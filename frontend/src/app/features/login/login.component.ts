import { Component } from '@angular/core';
import { BannerComponent } from '../register/components/banner/banner.component';
import { GlassBackgroundComponent } from '../../shared/ui/glass-background/glass-background.component';
import { LoginFormComponent } from './components/login-form/login-form.component';

@Component({
  selector: 'app-login',
  imports: [BannerComponent, GlassBackgroundComponent, LoginFormComponent],
  templateUrl: './login.component.html',
})
export class LoginComponent {}
