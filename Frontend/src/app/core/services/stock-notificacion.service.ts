import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { InventarioService, ProductoResponse } from './inventario.service';

@Injectable({ providedIn: 'root' })
export class StockNotificacionService implements OnDestroy {
  private auth = inject(AuthService);
  private svc  = inject(InventarioService);

  productos = signal<ProductoResponse[]>([]);
  visible   = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;

  init(): void {
    this.check();
    this.interval = setInterval(() => this.check(), 5 * 60 * 1000);
  }

  dismiss(): void {
    this.visible.set(false);
  }

  private check(): void {
    const rol = this.auth.rol();
    if (rol !== 'ADMIN' && rol !== 'RECEPCION') {
      this.productos.set([]);
      this.visible.set(false);
      return;
    }
    this.svc.getProductosBajoStock().subscribe({
      next: lista => {
        this.productos.set(lista);
        if (lista.length > 0) this.visible.set(true);
      },
    });
  }

  ngOnDestroy(): void {
    if (this.interval) clearInterval(this.interval);
  }
}
