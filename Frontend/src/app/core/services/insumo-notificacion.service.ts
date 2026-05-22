import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { GroomingService } from './grooming.service';

@Injectable({ providedIn: 'root' })
export class InsumoNotificacionService implements OnDestroy {
  private auth = inject(AuthService);
  private svc  = inject(GroomingService);

  pendientes = signal(0);
  visible    = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;

  init(): void {
    this.check();
    this.interval = setInterval(() => this.check(), 5 * 60 * 1000);
  }

  dismiss(): void { this.visible.set(false); }

  private check(): void {
    if (this.auth.rol() !== 'RECEPCION') {
      this.pendientes.set(0);
      this.visible.set(false);
      return;
    }
    this.svc.getTodasEntregas().subscribe({
      next: lista => {
        const n = lista.filter(i => i.estado === 'SOLICITADO').length;
        this.pendientes.set(n);
        if (n > 0) this.visible.set(true);
      },
    });
  }

  ngOnDestroy(): void {
    if (this.interval) clearInterval(this.interval);
  }
}
