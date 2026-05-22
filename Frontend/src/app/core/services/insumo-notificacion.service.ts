import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService } from './auth.service';
import { GroomingService } from './grooming.service';
import { StockSseService } from './stock-sse.service';

@Injectable({ providedIn: 'root' })
export class InsumoNotificacionService implements OnDestroy {
  private auth = inject(AuthService);
  private svc  = inject(GroomingService);
  private sse  = inject(StockSseService);

  pendientes = signal(0);
  visible    = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;
  private sseSub: Subscription | null = null;

  init(): void {
    this.refresh();
    this.interval = setInterval(() => this.refresh(), 30_000);
    this.sseSub = this.sse.insumoChanged$.subscribe(() => this.refresh());
  }

  dismiss(): void { this.visible.set(false); }

  refresh(): void {
    if (this.auth.rol() !== 'RECEPCION') {
      this.pendientes.set(0);
      this.visible.set(false);
      return;
    }
    this.svc.getTodasEntregas().subscribe({
      next: lista => {
        const n = lista.filter(i => i.estado === 'SOLICITADO').length;
        this.pendientes.set(n);
        if (n > 0) {
          this.visible.set(true);
          this.sendDesktopNotif(n);
        } else {
          this.visible.set(false);
        }
      },
    });
  }

  private async sendDesktopNotif(n: number): Promise<void> {
    if (typeof Notification === 'undefined') return;
    if (Notification.permission === 'default') await Notification.requestPermission();
    if (Notification.permission !== 'granted') return;
    new Notification('SpaMascotas — Insumos solicitados', {
      body: `${n} insumo${n > 1 ? 's' : ''} esperando aprobación`,
      icon: '/favicon.ico',
    });
  }

  ngOnDestroy(): void {
    if (this.interval) clearInterval(this.interval);
    this.sseSub?.unsubscribe();
  }
}
