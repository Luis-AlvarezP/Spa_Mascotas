import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environments/environment';

interface AuditLog {
  id: number;
  accion: string;
  accionLabel: string | null;
  correoUsuario: string | null;
  rol: string | null;
  ip: string | null;
  userAgent: string | null;
  detalles: string | null;
  exitoso: boolean;
  timestamp: string;
}

interface PageResponse {
  content: AuditLog[];
  totalElements: number;
  totalPages: number;
  number: number;
}

const ACCIONES: { label: string; value: string }[] = [
  { label: 'Todas las acciones',              value: '' },
  { label: 'Inicio de sesión exitoso',        value: 'LOGIN_EXITOSO' },
  { label: 'Inicio de sesión fallido',        value: 'LOGIN_FALLIDO' },
  { label: 'Cuenta bloqueada',                value: 'CUENTA_BLOQUEADA' },
  { label: 'Registro de cuenta',              value: 'REGISTRO' },
  { label: 'Activación de cuenta',            value: 'ACTIVACION_CUENTA' },
  { label: 'Cierre de sesión',                value: 'LOGOUT' },
  { label: 'Cambio de contraseña',            value: 'CAMBIO_PASSWORD' },
  { label: 'Restablecimiento de contraseña',  value: 'RESETEO_PASSWORD' },
  { label: 'Creación de personal',            value: 'CREAR_STAFF' },
  { label: 'Modificación de usuario',         value: 'MODIFICAR_USUARIO' },
  { label: 'Activación de usuario',           value: 'ACTIVAR_USUARIO' },
  { label: 'Desactivación de usuario',        value: 'DESACTIVAR_USUARIO' },
  { label: 'Acceso a módulo',                 value: 'ACCESO_MODULO' },
  { label: 'Movimiento de inventario',        value: 'MOVIMIENTO_INVENTARIO' },
  { label: 'Venta realizada',                 value: 'VENTA_REALIZADA' },
  { label: 'Venta de servicio',               value: 'VENTA_SERVICIO' },
];
const ROLES = ['Todos', 'ADMIN', 'RECEPCION', 'GROOMER', 'CLIENTE'];

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [RouterLink, DatePipe, FormsModule],
  templateUrl: './audit.component.html',
  styleUrl: './audit.component.scss',
})
export class AuditComponent implements OnInit {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/admin/audit`;

  logs          = signal<AuditLog[]>([]);
  totalElements = signal(0);
  totalPages    = signal(0);
  cargando      = signal(false);
  exportando    = signal(false);
  error         = signal<string | null>(null);

  page        = signal(0);
  rolFiltro   = signal('');
  accionFiltro= signal('');
  desde       = signal('');
  hasta       = signal('');

  readonly acciones = ACCIONES;
  readonly roles    = ROLES;

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);

    const params: Record<string, string> = {
      page: String(this.page()),
      size: '50',
    };
    if (this.rolFiltro())    params['rol']    = this.rolFiltro();
    if (this.accionFiltro()) params['accion'] = this.accionFiltro();
    if (this.desde())        params['desde']  = this.desde();
    if (this.hasta())        params['hasta']  = this.hasta();

    this.http.get<PageResponse>(this.apiUrl, { params }).subscribe({
      next: data => {
        this.logs.set(data.content);
        this.totalElements.set(data.totalElements);
        this.totalPages.set(data.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('Error al cargar los logs de auditoría');
        this.cargando.set(false);
      },
    });
  }

  filtrarRol(rol: string): void {
    this.rolFiltro.set(rol === 'Todos' ? '' : rol);
    this.page.set(0);
    this.cargar();
  }

  filtrarAccion(accion: string): void {
    this.accionFiltro.set(accion);
    this.page.set(0);
    this.cargar();
  }

  aplicarFechas(): void {
    this.page.set(0);
    this.cargar();
  }

  limpiarFechas(): void {
    this.desde.set('');
    this.hasta.set('');
    this.page.set(0);
    this.cargar();
  }

  cambiarPagina(delta: number): void {
    const nueva = this.page() + delta;
    if (nueva >= 0 && nueva < this.totalPages()) {
      this.page.set(nueva);
      this.cargar();
    }
  }

  exportarExcel(): void {
    this.exportando.set(true);
    const params: Record<string, string> = {};
    if (this.rolFiltro())    params['rol']    = this.rolFiltro();
    if (this.accionFiltro()) params['accion'] = this.accionFiltro();
    if (this.desde())        params['desde']  = this.desde();
    if (this.hasta())        params['hasta']  = this.hasta();

    const query = new URLSearchParams(params).toString();
    const url = `${this.apiUrl}/export${query ? '?' + query : ''}`;

    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `audit_log_${new Date().toISOString().slice(0,10)}.xlsx`;
        a.click();
        URL.revokeObjectURL(a.href);
        this.exportando.set(false);
      },
      error: () => this.exportando.set(false),
    });
  }

  badgeClass(log: AuditLog): string {
    if (!log.exitoso) return 'badge-error';
    const a = log.accion;
    if (a === 'LOGIN_EXITOSO') return 'badge-success';
    if (a === 'REGISTRO' || a === 'ACTIVACION_CUENTA') return 'badge-info';
    if (a === 'CREAR_STAFF' || a === 'MODIFICAR_USUARIO') return 'badge-purple';
    if (a === 'ACTIVAR_USUARIO') return 'badge-success';
    if (a === 'DESACTIVAR_USUARIO') return 'badge-warning';
    if (a === 'CUENTA_BLOQUEADA') return 'badge-warning';
    if (a === 'VENTA_REALIZADA' || a === 'VENTA_SERVICIO') return 'badge-green';
    if (a === 'MOVIMIENTO_INVENTARIO') return 'badge-teal';
    return 'badge-neutral';
  }

  parseBrowser(ua: string): string {
    if (!ua || ua === 'desconocido') return '—';
    if (/Edg\//.test(ua))    return 'Edge';
    if (/OPR\//.test(ua))    return 'Opera';
    if (/Chrome\//.test(ua)) return 'Chrome';
    if (/Firefox\//.test(ua))return 'Firefox';
    if (/Safari\//.test(ua)) return 'Safari';
    return 'Otro';
  }
}
