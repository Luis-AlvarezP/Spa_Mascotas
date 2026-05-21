import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ClienteListResponse {
  id: number;
  usuarioId: number;
  correo: string;
  nombre: string;
  ci: string | null;
  telefono: string | null;
  direccion: string | null;
  estado: string;
  mascotas: string[];
}

export interface TemperamentoResponse {
  id: number;
  nombre: string;
  colorAlerta: string;
}

export interface RestriccionResponse {
  id: number;
  tipo: string;
  descripcion: string;
  gravedad: string;
}

export interface MascotaResponse {
  id: number;
  nombre: string;
  especie: string;
  raza?: string;
  tamano: string;
  fechaNacimiento?: string;
  alergias?: string;
  temperamentoId?: number;
  temperamentoNombre?: string;
  temperamentoColor?: string;
  urlFotoMascota?: string;
  urlCarnet?: string;
  activa?: boolean;
  restricciones?: RestriccionResponse[];
}

export interface RestriccionRequest {
  tipo: string;
  descripcion: string;
  gravedad: string;
}

export interface MascotaRequest {
  nombre: string;
  especie: string;
  raza?: string;
  tamano: string;
  fechaNacimiento?: string;
  alergias?: string;
  temperamentoId?: number | null;
  restricciones?: RestriccionRequest[];
}

@Injectable({ providedIn: 'root' })
export class MascotasService {
  private http = inject(HttpClient);
  private base = '/api/mascotas';

  temperamentos(): Observable<TemperamentoResponse[]> {
    return this.http.get<TemperamentoResponse[]>(`${this.base}/temperamentos`);
  }

  misMascotas(): Observable<MascotaResponse[]> {
    return this.http.get<MascotaResponse[]>(this.base);
  }

  registrar(req: MascotaRequest): Observable<MascotaResponse> {
    return this.http.post<MascotaResponse>(this.base, req);
  }

  actualizar(id: number, req: MascotaRequest): Observable<MascotaResponse> {
    return this.http.put<MascotaResponse>(`${this.base}/${id}`, req);
  }

  toggleActiva(id: number): Observable<MascotaResponse> {
    return this.http.patch<MascotaResponse>(`${this.base}/${id}/toggle`, {});
  }

  subirCarnet(id: number, file: File): Observable<{ url: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ url: string }>(`${this.base}/${id}/carnet`, form);
  }

  subirFoto(id: number, file: File): Observable<{ url: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ url: string }>(`${this.base}/${id}/foto`, form);
  }

  mascotasByCliente(clienteId: number): Observable<MascotaResponse[]> {
    return this.http.get<MascotaResponse[]>(`${this.base}/cliente/${clienteId}`);
  }

  listarClientes(): Observable<ClienteListResponse[]> {
    return this.http.get<ClienteListResponse[]>('/api/clientes');
  }
}
