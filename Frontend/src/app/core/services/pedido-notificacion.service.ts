import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService } from './auth.service';
import { InventarioService } from './inventario.service';
import { StockSseService } from './stock-sse.service';

@Injectable({ providedIn: 'root' })
export class PedidoNotificacionService implements OnDestroy {
  private auth = inject(AuthService);
  private svc  = inject(InventarioService);
  private sse  = inject(StockSseService);

  pendientes = signal(0);
  visible    = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;
  private sseSub: Subscription | null = null;

  init(): void {
    this.refresh();
    this.interval = setInterval(() => this.refresh(), 30_000);
    this.sseSub = this.sse.pedidoChanged$.subscribe(() => this.refresh());
  }

  dismiss(): void {
    this.visible.set(false);
  }

  refresh(): void {
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
        } else {
          this.visible.set(false);
        }
      },
    });
  }

  ngOnDestroy(): void {
    if (this.interval) clearInterval(this.interval);
    this.sseSub?.unsubscribe();
  }
}
