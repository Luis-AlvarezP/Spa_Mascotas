import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CitaNotificacionService } from '../../../core/services/cita-notificacion.service';

@Component({
  selector: 'app-cita-toast',
  standalone: true,
  templateUrl: './cita-toast.component.html',
  styleUrl: './cita-toast.component.scss',
})
export class CitaToastComponent {
  notif  = inject(CitaNotificacionService);
  router = inject(Router);

  irAAgenda(): void {
    this.notif.dismiss();
    this.router.navigate(['/agenda']);
  }
}
