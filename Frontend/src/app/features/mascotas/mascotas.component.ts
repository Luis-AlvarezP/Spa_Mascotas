import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-mascotas',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './mascotas.component.html',
  styleUrl: './mascotas.component.scss',
})
export class MascotasComponent {
  auth = inject(AuthService);
  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
  titulo    = computed(() => this.isCliente() ? 'Mis Mascotas' : 'Clientes y Mascotas');
  subtitulo = computed(() => this.isCliente()
    ? 'Administra los perfiles de tus mascotas'
    : 'Perfiles de clientes, mascotas e historial clínico');
}
