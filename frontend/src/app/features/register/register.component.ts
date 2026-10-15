import { Component } from '@angular/core';
import { RegisterFormComponent } from './components/register-form/register-form.component';
import { BannerComponent } from './components/banner/banner.component';
import { GlassBackgroundComponent } from '../../shared/ui/glass-background/glass-background.component';

@Component({
  selector: 'app-register',
  imports: [RegisterFormComponent, BannerComponent, GlassBackgroundComponent],
  templateUrl: './register.component.html',
})
export class RegisterComponent {}
