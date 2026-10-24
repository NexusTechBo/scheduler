import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { BannerComponent } from '../../shared/ui/banner/banner.component';
import { GlassBackgroundComponent } from '../../shared/ui/glass-background/glass-background.component';
import { RegisterFormComponent } from './components/register-form/register-form.component';
import { RegisterSuccessComponent } from './components/register-success/register-success.component';
import { RegisterService } from './register.service';

@Component({
  selector: 'app-register',
  imports: [
    RegisterFormComponent,
    RegisterSuccessComponent,
    BannerComponent,
    GlassBackgroundComponent,
  ],
  providers: [RegisterService],
  templateUrl: './register.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  protected readonly register = inject(RegisterService);
}
