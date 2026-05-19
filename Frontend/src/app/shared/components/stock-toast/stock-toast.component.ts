import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { StockNotificacionService } from '../../../core/services/stock-notificacion.service';

@Component({
  selector: 'app-stock-toast',
  standalone: true,
  templateUrl: './stock-toast.component.html',
  styleUrl: './stock-toast.component.scss',
})
export class StockToastComponent {
  notif  = inject(StockNotificacionService);
  router = inject(Router);

  irAInventario(): void {
    this.notif.dismiss();
    this.router.navigate(['/inventario']);
  }
}
