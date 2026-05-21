import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface GroomerResponse {
  id: number;
  nombre: string;
  correo: string;
  activo: boolean;
}

export interface HorarioTrabajo {
  id: number;
  empleadoId: number;
  empleadoNombre: string;
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
  inicioAlmuerzo?: string;
  finAlmuerzo?: string;
  vigenteDesde: string;
  vigenteHasta?: string;
  capacidadMaxima: number;
}

export interface HorarioRequest {
  empleadoId: number;
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
  inicioAlmuerzo?: string;
  finAlmuerzo?: string;
  vigenteDesde: string;
  vigenteHasta?: string;
  capacidadMaxima: number;
}

export interface Bloqueo {
  id: number;
  titulo: string;
  tipo: string;
  fechaInicio: string;
  fechaFin: string;
  empleadoId?: number;
  empleadoNombre?: string;
  descripcion?: string;
  creadoPorCorreo?: string;
}

export interface BloqueoRequest {
  titulo: string;
  tipo: string;
  fechaInicio: string;
  fechaFin: string;
  empleadoId?: number;
  descripcion?: string;
}

@Injectable({ providedIn: 'root' })
export class AgendaService {
  private http = inject(HttpClient);
  private base = '/api/agenda';

  groomers(): Observable<GroomerResponse[]> {
    return this.http.get<GroomerResponse[]>(`${this.base}/groomers`);
  }

  horariosPorGroomer(empleadoId: number): Observable<HorarioTrabajo[]> {
    return this.http.get<HorarioTrabajo[]>(`${this.base}/groomers/${empleadoId}/horarios`);
  }

  crearHorario(req: HorarioRequest): Observable<HorarioTrabajo> {
    return this.http.post<HorarioTrabajo>(`${this.base}/horarios`, req);
  }

  actualizarHorario(id: number, req: HorarioRequest): Observable<HorarioTrabajo> {
    return this.http.put<HorarioTrabajo>(`${this.base}/horarios/${id}`, req);
  }

  eliminarHorario(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/horarios/${id}`);
  }

  bloqueos(): Observable<Bloqueo[]> {
    return this.http.get<Bloqueo[]>(`${this.base}/bloqueos`);
  }

  crearBloqueo(req: BloqueoRequest): Observable<Bloqueo> {
    return this.http.post<Bloqueo>(`${this.base}/bloqueos`, req);
  }

  actualizarBloqueo(id: number, req: BloqueoRequest): Observable<Bloqueo> {
    return this.http.put<Bloqueo>(`${this.base}/bloqueos/${id}`, req);
  }

  eliminarBloqueo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/bloqueos/${id}`);
  }

  misHorarios(): Observable<HorarioTrabajo[]> {
    return this.http.get<HorarioTrabajo[]>('/api/grooming/mis-horarios');
  }
}
