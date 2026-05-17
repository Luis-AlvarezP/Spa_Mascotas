import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-activacion',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './activacion.component.html',
  styleUrl: './activacion.component.scss',
})
export class ActivacionComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);

  estado = signal<'cargando' | 'exito' | 'error'>('cargando');
  mensaje = signal('');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.estado.set('error');
      this.mensaje.set('Token de activación no encontrado.');
      return;
    }
    this.authService.activar(token).subscribe({
      next: resp => { this.estado.set('exito'); this.mensaje.set(resp.mensaje); },
      error: err => { this.estado.set('error'); this.mensaje.set(err.error?.mensaje ?? 'Token inválido o expirado.'); },
    });
  }
}
