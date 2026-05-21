import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { CitaService } from './cita.service';

@Injectable({ providedIn: 'root' })
export class CitaNotificacionService implements OnDestroy {
  private auth    = inject(AuthService);
  private citaSvc = inject(CitaService);

  enRevision    = signal(0);
  pendientePago = signal(0);
  visible       = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;

  init(): void {
    this.check();
    this.interval = setInterval(() => this.check(), 5 * 60 * 1000);
  }

  dismiss(): void { this.visible.set(false); }

  private check(): void {
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
          this.sendDesktopNotif(rev, pago);
        }
      },
    });
  }

  private sendDesktopNotif(rev: number, pago: number): void {
    if (typeof Notification === 'undefined') return;
    if (Notification.permission === 'default') Notification.requestPermission();
    if (Notification.permission !== 'granted') return;
    const partes: string[] = [];
    if (rev  > 0) partes.push(`${rev} cita${rev > 1 ? 's' : ''} por revisar`);
    if (pago > 0) partes.push(`${pago} cita${pago > 1 ? 's' : ''} por cobrar`);
    new Notification('SpaMascotas — Citas pendientes', {
      body: partes.join(' · '),
      icon: '/favicon.ico',
    });
  }

  ngOnDestroy(): void {
    if (this.interval) clearInterval(this.interval);
  }
}
