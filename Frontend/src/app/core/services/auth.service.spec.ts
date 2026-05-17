import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthResponse } from '../../models/auth.model';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  it('isAuthenticated debe ser false al iniciar sin sesión', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('login debe guardar el token y marcar autenticado', () => {
    const mockResp: AuthResponse = {
      accessToken: 'jwt.token',
      correo: 'user@test.com',
      rol: 'CLIENTE',
      requiere2fa: false,
    };

    service.login({ correo: 'user@test.com', password: 'Pass1234!' }).subscribe(resp => {
      expect(resp.accessToken).toBe('jwt.token');
    });

    http.expectOne('http://localhost:8080/api/auth/login').flush(mockResp);
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.rol()).toBe('CLIENTE');
  });

  it('logout debe limpiar la sesión', () => {
    const mockResp: AuthResponse = {
      accessToken: 'jwt.token',
      correo: 'user@test.com',
      rol: 'CLIENTE',
      requiere2fa: false,
    };
    service.login({ correo: 'user@test.com', password: 'Pass1234!' }).subscribe();
    http.expectOne('http://localhost:8080/api/auth/login').flush(mockResp);

    service.logout();
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.getAccessToken()).toBeNull();
  });
});
