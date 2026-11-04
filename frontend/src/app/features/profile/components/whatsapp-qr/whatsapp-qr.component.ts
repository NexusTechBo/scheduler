import { DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ConfirmService } from '../../../../shared/services/confirm.service';
import { DialogFrameComponent } from '../../../../shared/ui/dialog-frame/dialog-frame.component';
import { LoadErrorComponent } from '../../../../shared/ui/load-error/load-error.component';
import { SkeletonComponent } from '../../../../shared/ui/skeleton/skeleton.component';
import { WhatsappQrService } from './whatsapp-qr.service';

@Component({
  selector: 'app-whatsapp-qr',
  imports: [DialogFrameComponent, LoadErrorComponent, SkeletonComponent],
  providers: [WhatsappQrService],
  templateUrl: './whatsapp-qr.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WhatsappQrComponent {
  private readonly confirm = inject(ConfirmService);
  protected readonly whatsapp = inject(WhatsappQrService);
  protected readonly ref = inject(DialogRef);

  protected logout(): void {
    if (this.whatsapp.loggingOut()) return;
    this.confirm
      .confirmThen(
        {
          title: 'Desvincular WhatsApp',
          message:
            'Los pacientes dejarán de recibir avisos de sus citas por WhatsApp hasta que vuelvas a vincular un número.',
          confirmLabel: 'Desvincular',
          danger: true,
        },
        () => this.whatsapp.logout(),
        'WhatsApp desvinculado',
        'No se pudo desvincular WhatsApp',
      )
      .subscribe();
  }
}
