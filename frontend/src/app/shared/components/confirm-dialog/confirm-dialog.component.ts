import { Component, inject, signal } from '@angular/core';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.scss'
})
export class ConfirmDialogComponent {
  readonly visible = signal(false);
  readonly options = signal<ConfirmOptions | null>(null);

  private resolveRef?: (result: boolean) => void;

  open(opts: ConfirmOptions): Promise<boolean> {
    this.options.set(opts);
    this.visible.set(true);
    return new Promise(resolve => { this.resolveRef = resolve; });
  }

  confirm(): void { this.close(true); }
  cancel(): void { this.close(false); }

  private close(result: boolean): void {
    this.visible.set(false);
    this.resolveRef?.(result);
    this.resolveRef = undefined;
  }
}
