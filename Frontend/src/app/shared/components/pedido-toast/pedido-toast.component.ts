import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { PedidoNotificacionService } from '../../../core/services/pedido-notificacion.service';

@Component({
  selector: 'app-pedido-toast',
  standalone: true,
  templateUrl: './pedido-toast.component.html',
  styleUrl: './pedido-toast.component.scss',
})
export class PedidoToastComponent {
  notif  = inject(PedidoNotificacionService);
  router = inject(Router);

  irAPedidos(): void {
    this.notif.dismiss();
    this.router.navigate(['/inventario']);
  }
}
