import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

interface Categoria {
  id: string;
  label: string;
  acciones: string[];
  rolFiltro?: string;
}

const CATEGORIAS: Categoria[] = [
  {
    id: 'personal',
    label: 'Creación de personal',
    acciones: ['CREAR_STAFF'],
  },
  {
    id: 'modificacion',
    label: 'Modificación de usuario',
    acciones: ['MODIFICAR_USUARIO', 'ACTIVAR_USUARIO', 'DESACTIVAR_USUARIO'],
  },
  {
    id: 'inventario',
    label: 'Movimientos de inventario',
    acciones: ['MOVIMIENTO_INVENTARIO', 'CREAR_PRODUCTO', 'MODIFICAR_PRODUCTO', 'DESACTIVAR_PRODUCTO'],
  },
  {
    id: 'ventas',
    label: 'Ventas',
    acciones: ['VENTA_REALIZADA', 'VENTA_SERVICIO'],
  },
];

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

  readonly categorias = CATEGORIAS;

  logs            = signal<AuditLog[]>([]);
  totalElements   = signal(0);
  totalPages      = signal(0);
  cargando        = signal(false);
  exportando      = signal(false);
  error           = signal<string | null>(null);
  categoriaActiva = signal<Categoria>(CATEGORIAS[0]);

  page  = signal(0);
  desde = signal('');
  hasta = signal('');

  ngOnInit(): void { this.cargar(); }

  cambiarCategoria(cat: Categoria): void {
    this.categoriaActiva.set(cat);
    this.page.set(0);
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);

    let params = new HttpParams()
      .set('page', String(this.page()))
      .set('size', '50');

    for (const a of this.categoriaActiva().acciones) {
      params = params.append('acciones', a);
    }
    if (this.categoriaActiva().rolFiltro) params = params.set('rol', this.categoriaActiva().rolFiltro!);
    if (this.desde()) params = params.set('desde', this.desde());
    if (this.hasta()) params = params.set('hasta', this.hasta());

    this.http.get<PageResponse>(this.apiUrl, { params }).subscribe({
      next: data => {
        this.logs.set(data.content);
        this.totalElements.set(data.totalElements);
        this.totalPages.set(data.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('Error al cargar los registros');
        this.cargando.set(false);
      },
    });
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
    let params = new HttpParams();
    for (const a of this.categoriaActiva().acciones) {
      params = params.append('acciones', a);
    }
    if (this.categoriaActiva().rolFiltro) params = params.set('rol', this.categoriaActiva().rolFiltro!);
    if (this.desde()) params = params.set('desde', this.desde());
    if (this.hasta()) params = params.set('hasta', this.hasta());

    const query = params.toString();
    const url = `${this.apiUrl}/export${query ? '?' + query : ''}`;

    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `audit_${this.categoriaActiva().id}_${new Date().toISOString().slice(0,10)}.xlsx`;
        a.click();
        URL.revokeObjectURL(a.href);
        this.exportando.set(false);
      },
      error: () => this.exportando.set(false),
    });
  }

  badgeClass(accion: string): string {
    const map: Record<string, string> = {
      CREAR_STAFF:           'badge-purple',
      MODIFICAR_USUARIO:     'badge-info',
      ACTIVAR_USUARIO:       'badge-success',
      DESACTIVAR_USUARIO:    'badge-warning',
      MOVIMIENTO_INVENTARIO: 'badge-teal',
      CREAR_PRODUCTO:        'badge-teal',
      MODIFICAR_PRODUCTO:    'badge-info',
      DESACTIVAR_PRODUCTO:   'badge-warning',
      VENTA_REALIZADA:       'badge-green',
      VENTA_SERVICIO:        'badge-green',
    };
    return map[accion] ?? 'badge-neutral';
  }
}
