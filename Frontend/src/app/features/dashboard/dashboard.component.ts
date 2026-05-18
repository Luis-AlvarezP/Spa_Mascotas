import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  auth = inject(AuthService);

  isAdmin  = computed(() => this.auth.rol() === 'ADMIN');
  nombre   = computed(() => {
    const u = this.auth.usuario();
    if (u?.nombreUsuario) return u.nombreUsuario;
    return u?.correo?.split('@')?.[0] ?? 'Usuario';
  });
}
