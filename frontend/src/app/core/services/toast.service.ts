import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: string;
  message: string;
  type: ToastType;
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);

  show(message: string, type: ToastType = 'info', duration = 3500): void {
    const id = crypto.randomUUID();
    this.toasts.update(t => [...t, { id, message, type, duration }]);
    setTimeout(() => this.remove(id), duration);
  }

  success(message: string, duration?: number): void { this.show(message, 'success', duration); }
  error(message: string, duration?: number): void { this.show(message, 'error', duration ?? 5000); }
  info(message: string, duration?: number): void { this.show(message, 'info', duration); }
  warning(message: string, duration?: number): void { this.show(message, 'warning', duration); }

  remove(id: string): void {
    this.toasts.update(t => t.filter(toast => toast.id !== id));
  }
}
