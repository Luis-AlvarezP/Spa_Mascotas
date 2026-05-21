import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { CitaService } from './cita.service';

@Injectable({ providedIn: 'root' })
export class GroomingNotificacionService implements OnDestroy {
  private auth    = inject(AuthService);
  private citaSvc = inject(CitaService);

  confirmadas = signal(0);
  visible     = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;

  init(): void {
    this.check();
    this.interval = setInterval(() => this.check(), 5 * 60 * 1000);
  }

  dismiss(): void { this.visible.set(false); }

  private check(): void {
    if (this.auth.rol() !== 'GROOMER') {
      this.confirmadas.set(0);
      this.visible.set(false);
      return;
    }
    this.citaSvc.misServicios().subscribe({
      next: lista => {
        const count = lista.filter(c => c.estado === 'ACEPTADO').length;
        this.confirmadas.set(count);
        if (count > 0) {
          this.visible.set(true);
          this.sendDesktopNotif(count);
        }
      },
    });
  }

  private sendDesktopNotif(count: number): void {
    if (typeof Notification === 'undefined') return;
    if (Notification.permission === 'default') Notification.requestPermission();
    if (Notification.permission !== 'granted') return;
    new Notification('SpaMascotas — Servicios pendientes', {
      body: `Tienes ${count} servicio${count > 1 ? 's' : ''} por atender`,
      icon: '/favicon.ico',
    });
  }

  ngOnDestroy(): void {
    if (this.interval) clearInterval(this.interval);
  }
}
