import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService } from './auth.service';
import { InventarioService, ProductoResponse } from './inventario.service';
import { StockSseService } from './stock-sse.service';

@Injectable({ providedIn: 'root' })
export class StockNotificacionService implements OnDestroy {
  private auth = inject(AuthService);
  private svc  = inject(InventarioService);
  private sse  = inject(StockSseService);

  productos = signal<ProductoResponse[]>([]);
  visible   = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;
  private sseSub: Subscription | null = null;

  init(): void {
    this.refresh();
    this.interval = setInterval(() => this.refresh(), 30_000);
    this.sseSub = this.sse.stockChanged$.subscribe(() => this.refresh());
  }

  dismiss(): void {
    this.visible.set(false);
  }

  refresh(): void {
    const rol = this.auth.rol();
    if (rol !== 'ADMIN' && rol !== 'RECEPCION') {
      this.productos.set([]);
      this.visible.set(false);
      return;
    }
    this.svc.getProductosBajoStock().subscribe({
      next: lista => {
        this.productos.set(lista);
        if (lista.length > 0) {
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
