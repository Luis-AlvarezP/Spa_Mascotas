import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, ForgotPasswordRequest, LoginRequest, MessageResponse, PerfilResponse, PreferenciaResponse, RegisterRequest, ResetPasswordRequest, Setup2faResponse } from '../../models/auth.model';
import { environment } from '../../../environments/environment';

const AUTH_KEY = 'spa_auth';
const INACTIVIDAD_MS = 30 * 60 * 1000; // 30 minutos

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http   = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = `${environment.apiUrl}/auth`;

  private _auth = signal<AuthResponse | null>(this.cargarSesion());

  private inactividadTimer: ReturnType<typeof setTimeout> | null = null;

  // Correo pendiente de configurar 2FA (staff sin JWT aún)
  pendingSetupCorreo = signal<string | null>(null);
  pendingSetupPassword = signal<string | null>(null);

  readonly isAuthenticated = computed(() => !!this._auth()?.accessToken);
  readonly usuario          = computed(() => this._auth());
  readonly rol              = computed(() => this._auth()?.rol ?? null);

  // ── Operaciones de sesión ──────────────────────────────

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, req).pipe(
      tap(resp => {
        if (resp.accessToken) {
          this.guardarSesion(resp);
          this.resetInactividadTimer();
        }
      })
    );
  }

  register(req: RegisterRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/register`, req);
  }

  activar(token: string): Observable<MessageResponse> {
    return this.http.get<MessageResponse>(`${this.apiUrl}/activar`, { params: { token } });
  }

  logout(): void {
    this.cancelarTimer();
    localStorage.removeItem(AUTH_KEY);
    this._auth.set(null);
    this.pendingSetupCorreo.set(null);
    this.pendingSetupPassword.set(null);
    this.router.navigate(['/auth/login']);
  }

  getAccessToken(): string | null {
    return this._auth()?.accessToken ?? null;
  }

  // ── OAuth2 callback ───────────────────────────────────

  guardarSesionOauth(auth: AuthResponse): void {
    this.guardarSesion(auth);
    this.resetInactividadTimer();
  }

  // ── 2FA pre-login (para staff) ────────────────────────

  init2faSetup(correo: string, password: string): Observable<Setup2faResponse> {
    return this.http.post<Setup2faResponse>(`${this.apiUrl}/2fa/init`, { correo, password });
  }

  enable2fa(correo: string, password: string, codigo: number): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/2fa/enable`, { correo, password, codigo }).pipe(
      tap(resp => {
        if (resp.accessToken) {
          this.guardarSesion(resp);
          this.resetInactividadTimer();
        }
      })
    );
  }

  // ── Perfil ─────────────────────────────────────────────

  getPerfil(): Observable<PerfilResponse> {
    return this.http.get<PerfilResponse>(`${environment.apiUrl}/perfil`);
  }

  updatePerfil(data: { nombre?: string; ci?: string; telefono?: string; nombreUsuario?: string; direccion?: string }): Observable<PerfilResponse> {
    return this.http.put<PerfilResponse>(`${environment.apiUrl}/perfil`, data);
  }

  changePassword(passwordActual: string, nuevaPassword: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${environment.apiUrl}/perfil/cambiar-password`, { passwordActual, nuevaPassword });
  }

  init2faLoggedIn(): Observable<Setup2faResponse> {
    return this.http.get<Setup2faResponse>(`${environment.apiUrl}/perfil/2fa/init`);
  }

  enable2faLoggedIn(codigo: number): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${environment.apiUrl}/perfil/2fa/enable`, { codigo });
  }

  disable2fa(password: string): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${environment.apiUrl}/perfil/2fa`, { params: { password } });
  }

  // ── Recuperación de contraseña ─────────────────────────

  forgotPassword(identificador: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/forgot-password`, { identificador });
  }

  resetPassword(token: string, nuevaPassword: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/reset-password`, { token, nuevaPassword });
  }

  // ── Preferencias de cliente ────────────────────────────

  getPreferencias(): Observable<PreferenciaResponse[]> {
    return this.http.get<PreferenciaResponse[]>(`${environment.apiUrl}/perfil/preferencias`);
  }

  savePreferencia(nombre: string, valor: string): Observable<PreferenciaResponse> {
    return this.http.post<PreferenciaResponse>(`${environment.apiUrl}/perfil/preferencias`, { nombre, valor });
  }

  deletePreferencia(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/perfil/preferencias/${id}`);
  }

  // ── Inactividad (30 min sin actividad → logout) ────────

  resetInactividadTimer(): void {
    if (!this.isAuthenticated()) return;
    this.cancelarTimer();
    this.inactividadTimer = setTimeout(() => this.logout(), INACTIVIDAD_MS);
  }

  private cancelarTimer(): void {
    if (this.inactividadTimer !== null) {
      clearTimeout(this.inactividadTimer);
      this.inactividadTimer = null;
    }
  }

  // ── Persistencia ──────────────────────────────────────

  private guardarSesion(auth: AuthResponse): void {
    localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
    this._auth.set(auth);
  }

  private cargarSesion(): AuthResponse | null {
    try {
      const raw = localStorage.getItem(AUTH_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
