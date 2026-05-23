import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import * as XLSX from 'xlsx';
import { AuthService } from '../../core/services/auth.service';
import { CitaService, CitaResponse } from '../../core/services/cita.service';

interface ClienteInfo {
  descuentoPct: number;
  pedidosEntregados: number;
  pedidosParaSiguienteNivel: number;
  penalizacionPct: number;
}

interface ServicioHoy { citaId: number; mascota: string; servicio: string; horaInicio: string; horaFin: string; precio: number; }
interface InsumoHoy   { producto: string; cantidad: number; estado: string; mascota: string; hora: string; }
interface GroomerProductividad {
  totalServicios: number;
  servicios: ServicioHoy[];
  totalInsumosUnidades: number;
  insumos: InsumoHoy[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  auth     = inject(AuthService);
  http     = inject(HttpClient);
  citaSvc  = inject(CitaService);

  isAdmin   = computed(() => this.auth.rol() === 'ADMIN');
  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
  isGroomer = computed(() => this.auth.rol() === 'GROOMER');

  nombre = computed(() => {
    const u = this.auth.usuario();
    if (u?.nombreUsuario) return u.nombreUsuario;
    return u?.correo?.split('@')?.[0] ?? 'Usuario';
  });

  clienteInfo        = signal<ClienteInfo | null>(null);
  proximaCita        = signal<CitaResponse | null>(null);
  groomerProduc      = signal<GroomerProductividad | null>(null);

  fechaBolivia = (() => {
    const f = new Date(new Date().toLocaleString('en-US', { timeZone: 'America/La_Paz' }));
    return f.toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  })();

  ngOnInit(): void {
    if (this.isCliente()) {
      this.http.get<ClienteInfo>('/api/clientes/mi-info').subscribe({
        next: info => this.clienteInfo.set(info),
      });
      this.citaSvc.misCitas().subscribe({
        next: citas => {
          const ahora = new Date();
          const proxima = citas
            .filter(c => ['EN_REVISION','ACEPTADO'].includes(c.estado) && new Date(c.fechaHoraInicio) > ahora)
            .sort((a, b) => new Date(a.fechaHoraInicio).getTime() - new Date(b.fechaHoraInicio).getTime())[0] ?? null;
          this.proximaCita.set(proxima);
        },
      });
    }
    if (this.isGroomer()) {
      this.http.get<GroomerProductividad>('/api/grooming/mi-productividad').subscribe({
        next: p => this.groomerProduc.set(p),
      });
    }
  }

  progresoDescuento(): number {
    const info = this.clienteInfo();
    if (!info) return 0;
    return (info.pedidosEntregados % 10) * 10;
  }

  fmtDia(iso: string): string {
    return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric' });
  }

  fmtMes(iso: string): string {
    return new Date(iso).toLocaleDateString('es-ES', { month: 'short' }).replace('.', '');
  }

  fmtHora(iso: string): string {
    return new Date(iso).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }

  estadoLabel(estado: string): string {
    return estado === 'ACEPTADO' ? 'Confirmada' : 'En revisión';
  }

  exportarExcel(tipo: 'servicios' | 'insumos'): void {
    const p = this.groomerProduc();
    if (!p) return;

    const hoyFmt = new Date(new Date().toLocaleString('en-US', { timeZone: 'America/La_Paz' }))
      .toLocaleDateString('es-ES');

    let aoa: (string | number)[][];
    let nombreHoja: string;
    let nombreArchivo: string;

    if (tipo === 'servicios') {
      nombreHoja    = 'Productividad';
      nombreArchivo = `productividad-${hoyFmt.replace(/\//g, '-')}.xlsx`;
      const encabezado = ['Cita #', 'Mascota', 'Servicio', 'Hora inicio', 'Hora fin', 'Precio (Bs.)'];
      const datos = p.servicios.length
        ? p.servicios.map(s => [s.citaId, s.mascota, s.servicio, s.horaInicio, s.horaFin, s.precio])
        : [['Sin servicios realizados hoy']];
      aoa = [
        ['Productividad Individual'],
        ['Fecha:', hoyFmt],
        [`${p.totalServicios} servicio${p.totalServicios !== 1 ? 's' : ''} realizados hoy`],
        [],
        encabezado,
        ...datos,
      ];
    } else {
      nombreHoja    = 'Insumos';
      nombreArchivo = `insumos-${hoyFmt.replace(/\//g, '-')}.xlsx`;
      const encabezado = ['Insumo', 'Cantidad', 'Estado', 'Mascota', 'Hora'];
      const datos = p.insumos.length
        ? p.insumos.map(i => [i.producto, i.cantidad, i.estado, i.mascota, i.hora])
        : [['Sin insumos registrados hoy']];
      aoa = [
        ['Consumo de Insumos'],
        ['Fecha:', hoyFmt],
        [`${p.totalInsumosUnidades} unidad${p.totalInsumosUnidades !== 1 ? 'es' : ''} de insumos pedidos hoy`],
        [],
        encabezado,
        ...datos,
      ];
    }

    const ws = XLSX.utils.aoa_to_sheet(aoa);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, nombreHoja);
    XLSX.writeFile(wb, nombreArchivo);
  }
}
