import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './inventario.component.html',
  styleUrl: './inventario.component.scss',
})
export class InventarioComponent {
  auth = inject(AuthService);
  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
  subtitulo = computed(() => this.isCliente()
    ? 'Catálogo de productos y servicios disponibles'
    : 'Stock, movimientos, catálogo, ventas y compras');
}
