import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { LowerCasePipe } from '@angular/common';

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [RouterLink, LowerCasePipe],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.scss',
})
export class AgendaComponent {
  auth = inject(AuthService);
  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
  titulo    = computed(() => this.isCliente() ? 'Mis Citas' : 'Agenda');
  subtitulo = computed(() => this.isCliente()
    ? 'Reserva y gestiona tus citas en el spa'
    : 'Gestión de citas y calendario del spa');
}
