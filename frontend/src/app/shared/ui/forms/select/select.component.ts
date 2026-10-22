import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  forwardRef,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { POPOVER_POSITIONS } from '../../overlay-positions';
import { UiIconComponent, UiIconName } from '../../ui-icon/ui-icon.component';

export interface SelectOption<T> {
  value: T;
  label: string;
  disabled?: boolean;
}

let nextId = 0;

@Component({
  selector: 'app-select',
  imports: [CdkConnectedOverlay, CdkOverlayOrigin, UiIconComponent],
  templateUrl: './select.component.html',
  host: { class: 'block', '[attr.id]': 'null' },
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectComponent), multi: true },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectComponent<T> implements ControlValueAccessor {
  readonly options = input<SelectOption<T>[]>([]);
  readonly placeholder = input('Seleccionar');
  readonly icon = input<UiIconName | null>(null);
  readonly id = input<string | null>(null);

  protected readonly listboxId = `app-select-${nextId++}`;
  protected readonly positions = POPOVER_POSITIONS;
  protected readonly value = signal<T | null>(null);
  protected readonly disabled = signal(false);
  protected readonly isOpen = signal(false);
  protected readonly activeIndex = signal(-1);
  protected readonly width = signal(0);
  protected readonly selectedIndex = computed(() =>
    this.options().findIndex((option) => option.value === this.value()),
  );
  protected readonly selected = computed<SelectOption<T> | undefined>(
    () => this.options()[this.selectedIndex()],
  );

  private readonly trigger = viewChild.required<ElementRef<HTMLButtonElement>>('triggerEl');
  private query = '';
  private queryTimer?: ReturnType<typeof setTimeout>;
  private onChange: (value: T) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: T | null): void {
    this.value.set(value ?? null);
  }

  registerOnChange(fn: (value: T) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
    if (isDisabled) this.close();
  }

  protected optionId(index: number): string {
    return `${this.listboxId}-${index}`;
  }

  protected toggle(): void {
    if (this.isOpen()) this.close();
    else this.open();
  }

  protected open(): void {
    if (this.disabled() || this.isOpen()) return;
    this.width.set(this.trigger().nativeElement.offsetWidth);
    const selected = this.selectedIndex();
    this.activeIndex.set(selected >= 0 ? selected : this.next(-1, 1));
    this.isOpen.set(true);
  }

  protected close(): void {
    if (!this.isOpen()) return;
    this.isOpen.set(false);
    this.onTouched();
  }

  protected onBlur(): void {
    if (this.isOpen()) this.close();
    else this.onTouched();
  }

  protected hover(index: number): void {
    if (!this.options()[index]?.disabled) this.activeIndex.set(index);
  }

  protected choose(index: number): void {
    const option = this.options()[index];
    if (!option || option.disabled) return;
    if (option.value !== this.value()) {
      this.value.set(option.value);
      this.onChange(option.value);
    }
    this.close();
    this.trigger().nativeElement.focus();
  }

  protected onKeydown(event: KeyboardEvent): void {
    const open = this.isOpen();
    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowUp':
        event.preventDefault();
        if (!open) this.open();
        else this.moveTo(this.next(this.activeIndex(), event.key === 'ArrowDown' ? 1 : -1));
        return;
      case 'Home':
      case 'End':
        if (!open) return;
        event.preventDefault();
        this.moveTo(event.key === 'Home' ? this.next(-1, 1) : this.next(this.options().length, -1));
        return;
      case 'Enter':
      case ' ':
        event.preventDefault();
        if (open) this.choose(this.activeIndex());
        else this.open();
        return;
      case 'Escape':
        if (open) {
          event.preventDefault();
          this.close();
        }
        return;
      case 'Tab':
        this.close();
        return;
      default:
        if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
          this.typeahead(event.key);
        }
    }
  }

  protected onAttach(): void {
    requestAnimationFrame(() => this.scrollToActive(true));
  }

  private scrollToActive(center = false): void {
    const list = document.getElementById(this.listboxId);
    const option = list?.children[this.activeIndex()] as HTMLElement | undefined;
    if (!list || !option) return;
    if (center) list.scrollTop = option.offsetTop - (list.clientHeight - option.offsetHeight) / 2;
    else option.scrollIntoView({ block: 'nearest' });
  }

  private moveTo(index: number): void {
    this.activeIndex.set(index);
    this.scrollToActive();
  }

  private next(from: number, direction: 1 | -1): number {
    const items = this.options();
    for (let i = from + direction; i >= 0 && i < items.length; i += direction) {
      if (!items[i].disabled) return i;
    }
    return from >= 0 && from < items.length ? from : -1;
  }

  private typeahead(key: string): void {
    clearTimeout(this.queryTimer);
    this.queryTimer = setTimeout(() => (this.query = ''), 500);
    this.query += key.toLowerCase();
    this.open();

    const items = this.options();
    const start = Math.max(this.activeIndex(), 0) + (this.query.length === 1 ? 1 : 0);
    for (let n = 0; n < items.length; n++) {
      const i = (start + n) % items.length;
      if (!items[i].disabled && items[i].label.toLowerCase().startsWith(this.query)) {
        this.moveTo(i);
        return;
      }
    }
  }
}
