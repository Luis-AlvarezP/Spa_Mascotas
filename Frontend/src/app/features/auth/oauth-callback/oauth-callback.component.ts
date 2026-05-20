import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../models/auth.model';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  template: `
    <div style="display:flex;align-items:center;justify-content:center;height:100vh;color:var(--text-muted,#aaa);font-family:sans-serif">
      <span>Iniciando sesión con Google...</span>
    </div>
  `,
})
export class OAuthCallbackComponent implements OnInit {
  private route  = inject(ActivatedRoute);
  private router = inject(Router);
  private auth   = inject(AuthService);

  ngOnInit(): void {
    const token         = this.route.snapshot.queryParamMap.get('token');
    const correo        = this.route.snapshot.queryParamMap.get('correo');
    const rol           = this.route.snapshot.queryParamMap.get('rol');
    const nombreUsuario = this.route.snapshot.queryParamMap.get('nombreUsuario');
    const setup         = this.route.snapshot.queryParamMap.get('setup') === 'true';
    const error         = this.route.snapshot.queryParamMap.get('error');

    if (error) {
      this.router.navigate(['/auth/login'], { queryParams: { error } });
      return;
    }

    if (!token || !correo || !rol) {
      this.router.navigate(['/auth/login']);
      return;
    }

    const authResp: AuthResponse = {
      accessToken:      token,
      correo:           correo,
      nombreUsuario:    nombreUsuario ?? null,
      rol:              rol,
      requiere2fa:      false,
      requiere2faSetup: false,
      totpHabilitado:   false,
    };
    this.auth.guardarSesionOauth(authResp);

    if (setup) {
      this.router.navigate(['/perfil'], { queryParams: { onboarding: 'true' } });
    } else {
      this.router.navigate(['/dashboard']);
    }
  }
}
