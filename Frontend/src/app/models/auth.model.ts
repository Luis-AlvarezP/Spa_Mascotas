export interface LoginRequest {
  identificador: string;
  password: string;
  codigoTotp?: number;
}

export interface RegisterRequest {
  correo: string;
  password: string;
  nombre: string;
  nombreUsuario?: string;
  telefono?: string;
  ci?: string;
  direccion?: string;
}

export interface ForgotPasswordRequest {
  identificador: string;
}

export interface ResetPasswordRequest {
  token: string;
  nuevaPassword: string;
}

export interface PreferenciaResponse {
  id: number;
  nombre: string;
  valor: string;
}

export interface AuthResponse {
  accessToken:       string;
  refreshToken?:     string;
  correo:            string;
  nombreUsuario?:    string | null;
  rol:               string;
  requiere2fa:       boolean;
  requiere2faSetup:  boolean;
  totpHabilitado:    boolean;
}

export interface MessageResponse {
  mensaje: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface PerfilResponse {
  correo:         string;
  nombre:         string | null;
  nombreUsuario:  string | null;
  rol:            string;
  ci:             string | null;
  telefono:       string | null;
  direccion:      string | null;
  totpHabilitado: boolean;
  tieneGoogle:    boolean;
}

export interface Setup2faResponse {
  qrUrl:  string;
  secret: string;
}
