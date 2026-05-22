import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface FichaRequest {
  citaId: number;
  estadoIngreso?: string;
  observacionesGenerales?: string;
  detalles?: string[];
  checklistUnas?: boolean;
  checklistOidos?: boolean;
  checklistGlandulas?: boolean;
  checklistCorte?: boolean;
  checklistBano?: boolean;
  checklistPerfume?: boolean;
  checklistPeinado?: boolean;
  recomendacion?: string;
  proximaCitaSugerida?: string;
}

export interface FotoResponse {
  id: number;
  url: string;
  momento: string;
}

export interface FichaResponse {
  id: number;
  citaId: number;
  mascotaNombre: string;
  clienteNombre: string;
  servicioNombre: string;
  fechaCita: string;
  estadoIngreso: string;
  observacionesGenerales: string;
  detalles: string[];
  checklistUnas: boolean;
  checklistOidos: boolean;
  checklistGlandulas: boolean;
  checklistCorte: boolean;
  checklistBano: boolean;
  checklistPerfume: boolean;
  checklistPeinado: boolean;
  checklistCompletados: number;
  estado: string;
  creadoEn: string;
  recomendacion: string;
  proximaCitaSugerida: string;
  fotos: FotoResponse[];
}

export interface InsumoRequest {
  citaId: number;
  empleadoId: number;
  productoId: number;
  cantidad: number;
  notas?: string;
}

export interface InsumoResponse {
  id: number;
  citaId: number;
  mascotaNombre: string | null;
  servicioNombre: string | null;
  empleadoId: number;
  empleadoNombre: string | null;
  productoId: number;
  productoNombre: string;
  cantidad: number;
  estado: string;
  notas: string | null;
  fecha: string;
}

export interface CitaGroomingResponse {
  id: number;
  mascotaNombre: string;
  clienteNombre: string;
  servicioNombre: string;
  empleadoAsignadoId: number | null;
  empleadoAsignadoNombre: string;
  fechaHoraInicio: string;
  estado: string;
}

@Injectable({ providedIn: 'root' })
export class GroomingService {
  private http = inject(HttpClient);
  private base = '/api/grooming';

  getCitasActivas(): Observable<CitaGroomingResponse[]> {
    return this.http.get<CitaGroomingResponse[]>(`${this.base}/citas-activas`);
  }

  getAllCitasActivas(): Observable<CitaGroomingResponse[]> {
    return this.http.get<CitaGroomingResponse[]>(`${this.base}/citas-activas/todas`);
  }

  getCitasActivasPorGroomer(groomerId: number): Observable<CitaGroomingResponse[]> {
    return this.http.get<CitaGroomingResponse[]>(`${this.base}/citas-activas/${groomerId}`);
  }

  getFichaPorCita(citaId: number): Observable<FichaResponse | null> {
    return this.http.get<FichaResponse>(`${this.base}/fichas/cita/${citaId}`).pipe(
      catchError(err => err.status === 404 ? of(null) : throwError(() => err))
    );
  }

  guardarFicha(req: FichaRequest): Observable<FichaResponse> {
    return this.http.post<FichaResponse>(`${this.base}/fichas`, req);
  }

  cerrarFicha(fichaId: number): Observable<FichaResponse> {
    return this.http.post<FichaResponse>(`${this.base}/fichas/${fichaId}/cerrar`, {});
  }

  subirFoto(citaId: number, file: File, momento: string): Observable<FotoResponse> {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('momento', momento);
    return this.http.post<FotoResponse>(`${this.base}/fichas/${citaId}/fotos`, fd);
  }

  eliminarFoto(fotoId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/fotos/${fotoId}`);
  }

  getInsumosPorCita(citaId: number): Observable<InsumoResponse[]> {
    return this.http.get<InsumoResponse[]>(`${this.base}/insumos/cita/${citaId}`);
  }

  getTodasEntregas(): Observable<InsumoResponse[]> {
    return this.http.get<InsumoResponse[]>(`${this.base}/insumos`);
  }

  entregarInsumo(req: InsumoRequest): Observable<InsumoResponse> {
    return this.http.post<InsumoResponse>(`${this.base}/insumos`, req);
  }

  solicitarInsumo(req: InsumoRequest): Observable<InsumoResponse> {
    return this.http.post<InsumoResponse>(`${this.base}/insumos/solicitar`, req);
  }

  actualizarEstadoInsumo(movId: number, estado: string): Observable<InsumoResponse> {
    return this.http.put<InsumoResponse>(`${this.base}/insumos/${movId}/estado`, { estado });
  }
}
