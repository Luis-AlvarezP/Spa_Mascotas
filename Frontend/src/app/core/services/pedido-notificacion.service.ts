import { Injectable, OnDestroy, inject } from '@angular/core';
import { signal } from '@angular/core';
import { AuthService } from './auth.service';
import { InventarioService } from './inventario.service';

@Injectable({ providedIn: 'root' })
export class PedidoNotificacionService implements OnDestroy {
  private auth = inject(AuthService);
  private svc  = inject(InventarioService);

  pendientes = signal(0);
  visible    = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;

  init(): void {
    this.check();
    this.interval = setInterval(() => this.check(), 5 * 60 * 1000);
  }

  dismiss(): void {
    this.visible.set(false);
  }

  private check(): void {
    if (this.auth.rol() !== 'RECEPCION') {
      this.pendientes.set(0);
      this.visible.set(false);
      return;
    }
    this.svc.listarPedidosAdmin().subscribe({
      next: lista => {
        const n = lista.filter(p => p.estado === 'EN_ESPERA').length;
        this.pendientes.set(n);
        if (n > 0) {
          this.visible.set(true);
          this.sendDesktopNotif(n);
        }
      },
    });
  }

  private sendDesktopNotif(n: number): void {
    if (this.auth.rol() === 'ADMIN') return;
    if (typeof Notification === 'undefined') return;
    if (Notification.permission === 'default') Notification.requestPermission();
    if (Notification.permission === 'granted') {
      new Notification('SpaMascotas — Pedidos pendientes', {
        body: `${n} pedido${n > 1 ? 's' : ''} en espera de entrega`,
        icon: '/favicon.ico',
      });
    }
  }

  ngOnDestroy(): void {
    if (this.interval) clearInterval(this.interval);
  }
}
