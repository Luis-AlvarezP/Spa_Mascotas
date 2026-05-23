import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService } from './auth.service';
import { CitaService } from './cita.service';
import { StockSseService } from './stock-sse.service';

@Injectable({ providedIn: 'root' })
export class CitaNotificacionService implements OnDestroy {
  private auth    = inject(AuthService);
  private citaSvc = inject(CitaService);
  private sse     = inject(StockSseService);

  enRevision    = signal(0);
  pendientePago = signal(0);
  visible       = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;
  private sseSub: Subscription | null = null;

  init(): void {
    this.refresh();
    this.interval = setInterval(() => this.refresh(), 30_000);
    this.sseSub = this.sse.citaChanged$.subscribe(() => this.refresh());
  }

  dismiss(): void { this.visible.set(false); }

  refresh(): void {
    const rol = this.auth.rol();
    if (rol !== 'RECEPCION') {
      this.enRevision.set(0); this.pendientePago.set(0); this.visible.set(false);
      return;
    }
    this.citaSvc.todasLasCitas().subscribe({
      next: lista => {
        const rev  = lista.filter(c => c.estado === 'EN_REVISION').length;
        const pago = lista.filter(c => c.estado === 'PENDIENTE_PAGO').length;
        this.enRevision.set(rev);
        this.pendientePago.set(pago);
        if (rev + pago > 0) {
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
