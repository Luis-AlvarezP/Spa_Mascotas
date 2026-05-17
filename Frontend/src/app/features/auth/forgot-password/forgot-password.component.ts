import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
})
export class ForgotPasswordComponent {
  private fb   = inject(FormBuilder);
  private auth = inject(AuthService);

  cargando = signal(false);
  error    = signal<string | null>(null);
  enviado  = signal(false);

  form = this.fb.group({
    identificador: ['', Validators.required],
  });

  get idCtrl() { return this.form.get('identificador')!; }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.cargando.set(true);
    this.error.set(null);

    this.auth.forgotPassword(this.form.value.identificador!).subscribe({
      next: () => { this.cargando.set(false); this.enviado.set(true); },
      error: err => {
        this.cargando.set(false);
        this.error.set(err.error?.mensaje ?? 'Error al enviar el correo');
      },
    });
  }
}
