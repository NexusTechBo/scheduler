import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-skeleton',
  templateUrl: './skeleton.component.html',
  host: { class: 'contents' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SkeletonComponent {
  readonly rows = input(1);
  readonly heightClass = input('h-14');

  protected readonly rowList = computed(() => Array.from({ length: this.rows() }, (_, i) => i));
}
