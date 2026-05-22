import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { InsumoNotificacionService } from '../../../core/services/insumo-notificacion.service';

@Component({
  selector: 'app-insumo-toast',
  standalone: true,
  templateUrl: './insumo-toast.component.html',
  styleUrl: './insumo-toast.component.scss',
})
export class InsumoToastComponent {
  notif  = inject(InsumoNotificacionService);
  router = inject(Router);

  irAInventario(): void {
    this.notif.dismiss();
    this.router.navigate(['/inventario']);
  }
}
