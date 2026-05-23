import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService } from './auth.service';
import { CitaService } from './cita.service';
import { StockSseService } from './stock-sse.service';

@Injectable({ providedIn: 'root' })
export class GroomingNotificacionService implements OnDestroy {
  private auth    = inject(AuthService);
  private citaSvc = inject(CitaService);
  private sse     = inject(StockSseService);

  confirmadas = signal(0);
  visible     = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;
  private sseSub: Subscription | null = null;

  init(): void {
    this.refresh();
    this.interval = setInterval(() => this.refresh(), 30_000);
    this.sseSub = this.sse.groomingChanged$.subscribe(() => this.refresh());
  }

  dismiss(): void { this.visible.set(false); }

  refresh(): void {
    if (this.auth.rol() !== 'GROOMER') {
      this.confirmadas.set(0);
      this.visible.set(false);
      return;
    }
    this.citaSvc.misServicios().subscribe({
      next: lista => {
        const hoy = new Date();
        const count = lista.filter(c => {
          if (c.estado !== 'ACEPTADO' || !c.fechaHoraInicio) return false;
          const f = new Date(c.fechaHoraInicio as any);
          return !isNaN(f.getTime()) &&
                 f.getFullYear() === hoy.getFullYear() &&
                 f.getMonth()    === hoy.getMonth()    &&
                 f.getDate()     === hoy.getDate();
        }).length;
        this.confirmadas.set(count);
        if (count > 0) {
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
