import { Injectable, signal } from '@angular/core';

export interface ConfirmConfig {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

interface ConfirmState extends ConfirmConfig {
  resolve: (result: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly pending = signal<ConfirmState | null>(null);

  confirm(config: ConfirmConfig | string): Promise<boolean> {
    const c: ConfirmConfig = typeof config === 'string' ? { message: config } : config;
    return new Promise(resolve => {
      this.pending.set({ ...c, resolve });
    });
  }

  accept(): void {
    this.pending()?.resolve(true);
    this.pending.set(null);
  }

  cancel(): void {
    this.pending()?.resolve(false);
    this.pending.set(null);
  }
}
