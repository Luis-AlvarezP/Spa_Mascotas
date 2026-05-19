import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  protected authService = inject(AuthService);
  private router = inject(Router);

  cargando = signal(false);
  error = signal<string | null>(null);
  mostrarPassword = signal(false);
  requiere2fa = signal(false);

  form = this.fb.group({
    identificador: ['', Validators.required],
    password:      ['', Validators.required],
    codigoTotp:    [null as number | null],
  });

  get identificadorCtrl() { return this.form.get('identificador')!; }
  get passwordCtrl()      { return this.form.get('password')!; }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.cargando.set(true);
    this.error.set(null);

    const { identificador, password, codigoTotp } = this.form.value;
    this.authService.login({ identificador: identificador!, password: password!, codigoTotp: codigoTotp ?? undefined })
      .subscribe({
        next: resp => {
          this.cargando.set(false);
          if (resp.requiere2faSetup) {
            this.authService.pendingSetupCorreo.set(resp.correo ?? identificador!);
            this.authService.pendingSetupPassword.set(password!);
            this.router.navigate(['/auth/setup-2fa']);
          } else if (resp.requiere2fa) {
            this.requiere2fa.set(true);
          } else {
            this.router.navigate(['/dashboard']);
          }
        },
        error: err => {
          this.cargando.set(false);
          this.error.set(err.error?.mensaje ?? 'Error al iniciar sesión');
        },
      });
  }
}
