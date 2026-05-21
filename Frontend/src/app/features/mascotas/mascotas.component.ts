import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import {
  MascotasService, MascotaResponse, ClienteListResponse,
} from '../../core/services/mascotas.service';
import { SearchBarComponent } from '../../shared/components/search-bar/search-bar.component';

@Component({
  selector: 'app-mascotas',
  standalone: true,
  imports: [SearchBarComponent],
  templateUrl: './mascotas.component.html',
  styleUrl: './mascotas.component.scss',
})
export class MascotasComponent implements OnInit {
  auth        = inject(AuthService);
  mascotasSvc = inject(MascotasService);

  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
  isStaff   = computed(() => ['ADMIN', 'RECEPCION', 'GROOMER'].includes(this.auth.rol() ?? ''));
  titulo    = computed(() => this.isCliente() ? 'Mis Mascotas' : 'Clientes y Mascotas');
  subtitulo = computed(() => this.isCliente()
    ? 'Administra los perfiles de tus mascotas'
    : 'Perfiles de clientes y sus mascotas');

  // ── Staff ────────────────────────────────────────────────
  clientes        = signal<ClienteListResponse[]>([]);
  loadingClientes = signal(false);
  busqueda        = signal('');
  clienteAbierto  = signal<number | null>(null);
  mascotasDelCliente     = signal<MascotaResponse[]>([]);
  loadingMascotas = signal(false);

  clientesFiltrados = computed(() => {
    const q = this.busqueda().toLowerCase().trim();
    if (!q) return this.clientes();
    return this.clientes().filter(c =>
      c.nombre?.toLowerCase().includes(q) ||
      c.correo?.toLowerCase().includes(q) ||
      c.ci?.toLowerCase().includes(q) ||
      c.telefono?.includes(q)
    );
  });

  // ── Cliente ──────────────────────────────────────────────
  misMascotasList     = signal<MascotaResponse[]>([]);
  loadingMisMascotas  = signal(false);
  busquedaMascota     = signal('');

  mascotasFiltradas = computed(() => {
    const q = this.busquedaMascota().toLowerCase().trim();
    if (!q) return this.misMascotasList();
    return this.misMascotasList().filter(m => m.nombre.toLowerCase().includes(q));
  });

  ngOnInit() {
    if (this.isStaff()) this.cargarClientes();
    if (this.isCliente()) this.cargarMisMascotas();
  }

  cargarMisMascotas() {
    this.loadingMisMascotas.set(true);
    this.mascotasSvc.misMascotas().subscribe({
      next: m  => { this.misMascotasList.set(m); this.loadingMisMascotas.set(false); },
      error: () => this.loadingMisMascotas.set(false),
    });
  }

  cargarClientes() {
    this.loadingClientes.set(true);
    this.mascotasSvc.listarClientes().subscribe({
      next: c  => { this.clientes.set(c); this.loadingClientes.set(false); },
      error: () => this.loadingClientes.set(false),
    });
  }

  toggleCliente(id: number) {
    if (this.clienteAbierto() === id) {
      this.clienteAbierto.set(null);
      this.mascotasDelCliente.set([]);
      return;
    }
    this.clienteAbierto.set(id);
    this.mascotasDelCliente.set([]);
    this.loadingMascotas.set(true);
    this.mascotasSvc.mascotasByCliente(id).subscribe({
      next: m  => { this.mascotasDelCliente.set(m); this.loadingMascotas.set(false); },
      error: () => this.loadingMascotas.set(false),
    });
  }

  temperamentoColor(nombre: string | null | undefined): string {
    const map: Record<string, string> = {
      'Tranquilo': '#4ade80', 'Nervioso': '#fbbf24',
      'Agresivo': '#f87171', 'Inquieto': '#a78bfa',
    };
    return (nombre && map[nombre]) ? map[nombre] : '#94a3b8';
  }

  tamanoClass(t: string | null | undefined): string {
    if (!t) return '';
    const map: Record<string, string> = { PEQUEÑO: 'tam-p', MEDIANO: 'tam-m', GRANDE: 'tam-g', GIGANTE: 'tam-x' };
    return map[t.toUpperCase().trim()] ?? '';
  }

  edad(fecha: string | undefined): string {
    if (!fecha) return '—';
    const diff = Date.now() - new Date(fecha).getTime();
    const años = Math.floor(diff / (1000 * 60 * 60 * 24 * 365));
    if (años < 1) {
      const meses = Math.floor(diff / (1000 * 60 * 60 * 24 * 30));
      return meses <= 0 ? 'Recién nacido' : `${meses} mes${meses > 1 ? 'es' : ''}`;
    }
    return `${años} año${años > 1 ? 's' : ''}`;
  }
}
