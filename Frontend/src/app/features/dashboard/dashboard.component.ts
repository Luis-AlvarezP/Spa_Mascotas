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

interface CitaHoy {
  citaId: number;
  clienteNombre: string;
  clienteCi: string | null;
  mascotaNombre: string;
  mascotaEspecie: string | null;
  mascotaTamano: string | null;
  mascotaTemperamento: string | null;
  servicio: string;
  groomer: string;
  horaInicio: string;
  horaFin: string;
  estado: string;
  motivoCancelacion: string | null;
}

interface MovimientoCaja {
  tipo: string;
  descripcion: string;
  clienteNombre: string;
  clienteCi: string | null;
  metodoPago: string;
  monto: number;
  hora: string;
}

interface AdminMovimiento {
  fecha: string;
  tipo: string;
  descripcion: string;
  clienteNombre: string;
  clienteCi: string | null;
  metodoPago: string;
  monto: number;
}

interface RankingItem {
  nombre: string;
  cantidad: number;
  totalIngresos: number;
}

interface AdminVentas {
  totalServicios: number;
  totalProductos: number;
  totalGeneral: number;
  porMetodoPago: Record<string, number>;
  movimientos: AdminMovimiento[];
  rankingServicios: RankingItem[];
  rankingProductos: RankingItem[];
}

interface GroomerStats {
  id: number;
  nombre: string;
  promedio: number;
  total: number;
}

interface CalificacionItem {
  id: number;
  groomer: string;
  puntuacion: number;
  comentario: string | null;
  fecha: string;
  mascota: string;
  servicio: string;
  cliente: string;
}

interface AdminCalificaciones {
  groomers: GroomerStats[];
  calificaciones: CalificacionItem[];
  promedioGeneral: number;
  totalCalificaciones: number;
}

interface CajaDiaria {
  totalServicios: number;
  totalProductos: number;
  totalGeneral: number;
  porMetodoPago: Record<string, number>;
  movimientos: MovimientoCaja[];
}

interface RecepcionDashboard {
  cronograma: CitaHoy[];
  canceladas: CitaHoy[];
  caja: CajaDiaria;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  protected readonly Math   = Math;
  protected readonly Object = Object;

  auth     = inject(AuthService);
  http     = inject(HttpClient);
  citaSvc  = inject(CitaService);

  isAdmin      = computed(() => this.auth.rol() === 'ADMIN');
  isCliente    = computed(() => this.auth.rol() === 'CLIENTE');
  isGroomer    = computed(() => this.auth.rol() === 'GROOMER');
  isRecepcion  = computed(() => this.auth.rol() === 'RECEPCION');

  nombre = computed(() => {
    const u = this.auth.usuario();
    if (u?.nombreUsuario) return u.nombreUsuario;
    return u?.correo?.split('@')?.[0] ?? 'Usuario';
  });

  clienteInfo        = signal<ClienteInfo | null>(null);
  proximaCita        = signal<CitaResponse | null>(null);
  groomerProduc      = signal<GroomerProductividad | null>(null);
  recepcionDash      = signal<RecepcionDashboard | null>(null);
  adminVentas        = signal<AdminVentas | null>(null);
  adminCalifs        = signal<AdminCalificaciones | null>(null);
  adminFiltroDesde   = signal('');
  adminFiltroHasta   = signal('');
  adminGroomerId     = signal<number | null>(null);
  mostrarMovimientos = signal(false);

  fechaBolivia = (() => {
    const f = new Date(new Date().toLocaleString('en-US', { timeZone: 'America/La_Paz' }));
    return f.toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  })();

  metodosOrdenados = computed(() => {
    const caja = this.recepcionDash()?.caja;
    if (!caja) return [];
    return Object.entries(caja.porMetodoPago).sort((a, b) => b[1] - a[1]);
  });

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
    if (this.isRecepcion()) {
      this.http.get<RecepcionDashboard>('/api/recepcion/dashboard').subscribe({
        next: d => this.recepcionDash.set(d),
      });
    }
    if (this.isAdmin()) {
      this.loadAdminVentas();
      this.loadAdminCalificaciones();
    }
  }

  loadAdminVentas(): void {
    const desde = this.adminFiltroDesde();
    const hasta = this.adminFiltroHasta();
    const params: string[] = [];
    if (desde) params.push(`desde=${desde}`);
    if (hasta) params.push(`hasta=${hasta}`);
    const url = '/api/admin/reportes/ventas' + (params.length ? '?' + params.join('&') : '');
    this.http.get<AdminVentas>(url).subscribe({ next: v => this.adminVentas.set(v) });
  }

  loadAdminCalificaciones(): void {
    const id = this.adminGroomerId();
    const url = id ? `/api/admin/reportes/calificaciones?empleadoId=${id}` : '/api/admin/reportes/calificaciones';
    this.http.get<AdminCalificaciones>(url).subscribe({ next: c => this.adminCalifs.set(c) });
  }

  aplicarFiltroAdmin(desde: string, hasta: string): void {
    this.adminFiltroDesde.set(desde);
    this.adminFiltroHasta.set(hasta);
    this.loadAdminVentas();
  }

  limpiarFiltroAdmin(inputDesde: HTMLInputElement, inputHasta: HTMLInputElement): void {
    inputDesde.value = '';
    inputHasta.value = '';
    this.adminFiltroDesde.set('');
    this.adminFiltroHasta.set('');
    this.loadAdminVentas();
  }

  seleccionarGroomer(val: string): void {
    this.adminGroomerId.set(val ? +val : null);
    this.loadAdminCalificaciones();
  }

  estrellas(n: number): string {
    return '★'.repeat(n) + '☆'.repeat(5 - n);
  }

  fmtFiltroFecha(iso: string): string {
    if (!iso) return '';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  exportarVentasAdmin(): void {
    const v = this.adminVentas();
    if (!v) return;
    const hoyFmt = new Date(new Date().toLocaleString('en-US', { timeZone: 'America/La_Paz' }))
      .toLocaleDateString('es-ES');
    const desde = this.adminFiltroDesde();
    const hasta = this.adminFiltroHasta();
    const periodo = desde && hasta ? `${desde} al ${hasta}` : 'Todo el tiempo';

    const aoa: (string | number | null)[][] = [
      ['REPORTE DE VENTAS — SpaMascotas'],
      ['Generado:', hoyFmt],
      ['Período:', periodo],
      [],
      ['RESUMEN'],
      ['Total servicios grooming:', v.totalServicios],
      ['Total productos tienda:', v.totalProductos],
      ['TOTAL GENERAL:', v.totalGeneral],
      [],
      ['POR MÉTODO DE PAGO'],
      ['Método', 'Total (Bs.)'],
      ...Object.entries(v.porMetodoPago).map(([k, val]) => [k, val]),
      [],
      ['RANKING — TOP SERVICIOS'],
      ['Servicio', 'Cantidad', 'Total Bs.'],
      ...v.rankingServicios.map(r => [r.nombre, r.cantidad, r.totalIngresos]),
      [],
      ['RANKING — TOP PRODUCTOS'],
      ['Producto', 'Unidades', 'Total Bs.'],
      ...v.rankingProductos.map(r => [r.nombre, r.cantidad, r.totalIngresos]),
      [],
      ['DETALLE DE MOVIMIENTOS'],
      ['Fecha', 'Tipo', 'Descripción', 'Cliente', 'CI', 'Método de pago', 'Monto (Bs.)'],
      ...v.movimientos.map(m => [m.fecha, m.tipo, m.descripcion, m.clienteNombre, m.clienteCi ?? '—', m.metodoPago, m.monto]),
    ];

    const ws = XLSX.utils.aoa_to_sheet(aoa);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Ventas');
    XLSX.writeFile(wb, `reporte-ventas-${hoyFmt.replace(/\//g, '-')}.xlsx`);
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

  estadoCronogramaLabel(estado: string): string {
    const m: Record<string, string> = {
      EN_REVISION: 'En revisión', ACEPTADO: 'Confirmada', PENDIENTE_PAGO: 'Por cobrar',
    };
    return m[estado] ?? estado;
  }

  estadoCronogramaClass(estado: string): string {
    const m: Record<string, string> = {
      EN_REVISION: 'cr-rev', ACEPTADO: 'cr-ok', PENDIENTE_PAGO: 'cr-pay',
    };
    return m[estado] ?? '';
  }

  metodoPagoIcon(metodo: string): string {
    const m: Record<string, string> = {
      QR: '▦', EFECTIVO: '💵', TRANSFERENCIA: '⇄', TARJETA: '▭', 'SIN ESPECIFICAR': '?',
    };
    return m[metodo] ?? '·';
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

  exportarCancelaciones(): void {
    const d = this.recepcionDash();
    if (!d) return;
    const hoyFmt = new Date(new Date().toLocaleString('en-US', { timeZone: 'America/La_Paz' }))
      .toLocaleDateString('es-ES');
    const aoa: (string | number | null)[][] = [
      ['CANCELACIONES DEL DÍA'],
      ['Fecha:', hoyFmt],
      [`${d.canceladas.length} cancelación${d.canceladas.length !== 1 ? 'es' : ''}`],
      [],
      ['Hora', 'Cliente', 'CI', 'Mascota', 'Servicio', 'Motivo'],
      ...d.canceladas.map(c => [c.horaInicio, c.clienteNombre, c.clienteCi ?? '—', c.mascotaNombre, c.servicio, c.motivoCancelacion ?? '—']),
    ];
    const ws = XLSX.utils.aoa_to_sheet(aoa);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Cancelaciones');
    XLSX.writeFile(wb, `cancelaciones-${hoyFmt.replace(/\//g, '-')}.xlsx`);
  }

  exportarCaja(): void {
    const d = this.recepcionDash();
    if (!d) return;
    const hoyFmt = new Date(new Date().toLocaleString('en-US', { timeZone: 'America/La_Paz' }))
      .toLocaleDateString('es-ES');

    const resumen: (string | number)[][] = [
      ['CIERRE DE CAJA DIARIO'],
      ['Fecha:', hoyFmt],
      [],
      ['TOTALES POR MÉTODO DE PAGO'],
      ['Método', 'Total (Bs.)'],
      ...Object.entries(d.caja.porMetodoPago).map(([k, v]) => [k, v]),
      [],
      ['Total servicios:', d.caja.totalServicios],
      ['Total productos:', d.caja.totalProductos],
      ['TOTAL GENERAL:', d.caja.totalGeneral],
      [],
      ['DETALLE DE MOVIMIENTOS'],
      ['Hora', 'Tipo', 'Descripción', 'Cliente', 'CI', 'Método de pago', 'Monto (Bs.)'],
      ...d.caja.movimientos.map(m => [m.hora, m.tipo, m.descripcion, m.clienteNombre, m.clienteCi ?? '—', m.metodoPago, m.monto]),
    ];

    const ws = XLSX.utils.aoa_to_sheet(resumen);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Caja');
    XLSX.writeFile(wb, `caja-${hoyFmt.replace(/\//g, '-')}.xlsx`);
  }
}
