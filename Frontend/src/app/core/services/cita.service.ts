import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ServicioResponse {
  id: number;
  nombre: string;
  descripcion: string;
  duracionMinutos: number;
  precioBase: number;
  activo: boolean;
}

export interface SlotResponse {
  inicio: string;
  fin: string;
  disponible: boolean;
  groomers: { id: number; nombre: string }[];
}

export interface GroomerBasicResponse {
  id: number;
  nombre: string;
  correo: string;
  activo: boolean;
}

export interface CitaResponse {
  id: number;
  estado: string;
  mascotaId: number;
  mascotaNombre: string;
  mascotaEspecie: string | null;
  mascotaTamano: string;
  mascotaTemperamento: string | null;
  mascotaTemperamentoColor: string | null;
  servicioId: number;
  servicioNombre: string;
  duracionMinutos: number;
  empleadoAsignadoId: number | null;
  empleadoAsignadoNombre: string | null;
  empleadoPreferidoId: number | null;
  empleadoPreferidoNombre: string | null;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  precioFinal: number;
  recargoPorcentaje: number;
  penalizacionCliente: number;
  motivoCancelacion: string | null;
  notas: string | null;
  creadoEn: string;
}

export interface CitaRequest {
  mascotaId: number;
  servicioId: number;
  empleadoPreferidoId?: number;
  fechaHoraInicio: string;
  notas?: string;
}

export interface CancelacionRequest {
  motivo: string;
}

export interface ReprogramarRequest {
  fechaHoraInicio: string;
  notas?: string;
  empleadoPreferidoId?: number;
}

@Injectable({ providedIn: 'root' })
export class CitaService {
  private http = inject(HttpClient);
  private base = '/api/citas';

  servicios(): Observable<ServicioResponse[]> {
    return this.http.get<ServicioResponse[]>('/api/servicios');
  }

  groomers(): Observable<GroomerBasicResponse[]> {
    return this.http.get<GroomerBasicResponse[]>(`${this.base}/groomers`);
  }

  slots(fecha: string, servicioId: number, mascotaId: number, empleadoId?: number, excluirCitaId?: number): Observable<SlotResponse[]> {
    const params: Record<string, string | number> = { fecha, servicioId, mascotaId };
    if (empleadoId) params['empleadoId'] = empleadoId;
    if (excluirCitaId) params['excluirCitaId'] = excluirCitaId;
    return this.http.get<SlotResponse[]>(`${this.base}/slots`, { params });
  }

  solicitar(req: CitaRequest): Observable<CitaResponse> {
    return this.http.post<CitaResponse>(this.base, req);
  }

  misCitas(): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(`${this.base}/mis-citas`);
  }

  cancelar(id: number, req: CancelacionRequest): Observable<CitaResponse> {
    return this.http.patch<CitaResponse>(`${this.base}/${id}/cancelar`, req);
  }

  reprogramar(id: number, req: ReprogramarRequest): Observable<CitaResponse> {
    return this.http.patch<CitaResponse>(`${this.base}/${id}/reprogramar`, req);
  }

  todasLasCitas(): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(`${this.base}/todas`);
  }

  misServicios(): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(`${this.base}/mis-servicios`);
  }

  aceptar(id: number): Observable<CitaResponse> {
    return this.http.patch<CitaResponse>(`${this.base}/${id}/aceptar`, {});
  }

  finalizarServicio(id: number): Observable<CitaResponse> {
    return this.http.patch<CitaResponse>(`${this.base}/${id}/finalizar`, {});
  }

  cobrar(id: number): Observable<CitaResponse> {
    return this.http.patch<CitaResponse>(`${this.base}/${id}/cobrar`, {});
  }

  calcularDuracion(base: number, tamano: string, temperamento: string | null): number {
    let dur = base;
    const t = (tamano ?? '').toUpperCase().trim();
    const tp = (temperamento ?? '').toUpperCase().trim();
    if (t === 'MEDIANO' || t === 'MEDIANA') dur += 5;
    else if (t === 'GRANDE') dur += 10;
    else if (t === 'GIGANTE') dur += 15;
    if (tp === 'NERVIOSO' || tp === 'INQUIETO') dur += 5;
    else if (tp === 'AGRESIVO') dur += 10;
    return dur;
  }

  calcularPrecio(base: number, tamano: string, temperamento: string | null): number {
    let precio = base;
    const t = (tamano ?? '').toUpperCase().trim();
    const tp = (temperamento ?? '').toUpperCase().trim();
    if (t === 'MEDIANO' || t === 'MEDIANA') precio += 10;
    else if (t === 'GRANDE') precio += 15;
    else if (t === 'GIGANTE') precio += 20;
    if (tp === 'NERVIOSO' || tp === 'INQUIETO') precio += 5;
    else if (tp === 'AGRESIVO') precio += 10;
    return precio;
  }
}
