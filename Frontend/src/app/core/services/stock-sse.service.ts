import { Injectable, OnDestroy, inject } from '@angular/core';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class StockSseService implements OnDestroy {
  private auth = inject(AuthService);

  readonly stockChanged$    = new Subject<void>();
  readonly citaChanged$     = new Subject<void>();
  readonly pedidoChanged$   = new Subject<void>();
  readonly insumoChanged$   = new Subject<void>();
  readonly groomingChanged$ = new Subject<void>();

  private eventSource: EventSource | null = null;

  connect(): void {
    const token = this.auth.getAccessToken();
    if (!token) return;
    this.disconnect();
    const url = `${environment.apiUrl}/sse/stock?token=${encodeURIComponent(token)}`;
    this.eventSource = new EventSource(url);
    this.eventSource.addEventListener('stock-update',    () => this.stockChanged$.next());
    this.eventSource.addEventListener('cita-update',     () => this.citaChanged$.next());
    this.eventSource.addEventListener('pedido-update',   () => this.pedidoChanged$.next());
    this.eventSource.addEventListener('insumo-update',   () => this.insumoChanged$.next());
    this.eventSource.addEventListener('grooming-update', () => this.groomingChanged$.next());
  }

  disconnect(): void {
    this.eventSource?.close();
    this.eventSource = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
