import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';

interface ClienteItem {
  id: number;
  correo: string;
  nombre: string | null;
  ci: string | null;
  telefono: string | null;
  direccion: string | null;
  estado: string;
  mascotas: string[];
}

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.scss',
})
export class ClientesComponent implements OnInit {
  private http   = inject(HttpClient);
  private auth   = inject(AuthService);
  private apiUrl = `${environment.apiUrl}/clientes`;

  isAdmin   = computed(() => this.auth.rol() === 'ADMIN');
  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
  isStaff   = computed(() => ['ADMIN','RECEPCION','GROOMER'].includes(this.auth.rol() ?? ''));

  clientes   = signal<ClienteItem[]>([]);
  cargando   = signal(false);
  error      = signal<string | null>(null);
  exito      = signal<string | null>(null);
  toggleando = signal<number | null>(null);

  ngOnInit(): void {
    if (this.isStaff()) this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.http.get<ClienteItem[]>(this.apiUrl).subscribe({
      next: data => { this.clientes.set(data); this.cargando.set(false); },
      error: () => { this.error.set('Error al cargar clientes'); this.cargando.set(false); },
    });
  }

  toggleEstado(c: ClienteItem): void {
    if (!this.isAdmin()) return;
    this.toggleando.set(c.id);
    this.error.set(null);
    this.exito.set(null);

    this.http.patch<ClienteItem>(`${this.apiUrl}/${c.id}/toggle`, {}).subscribe({
      next: actualizado => {
        this.clientes.update(list => list.map(x => x.id === actualizado.id ? actualizado : x));
        this.exito.set(`Cuenta ${actualizado.estado === 'ACTIVO' ? 'habilitada' : 'inhabilitada'} correctamente`);
        this.toggleando.set(null);
      },
      error: () => {
        this.error.set('Error al cambiar el estado');
        this.toggleando.set(null);
      },
    });
  }

  verMascotas(_clienteId: number): void {
    // placeholder — sin acción por ahora
  }
}
