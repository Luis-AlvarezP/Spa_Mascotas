import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { startWith } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  cargando = signal(false);
  error = signal<string | null>(null);
  exito = signal(false);
  mostrarPassword = signal(false);

  form = this.fb.group({
    nombre:        ['', Validators.required],
    correo:        ['', [Validators.required, Validators.email]],
    nombreUsuario: [''],
    ci:            [''],
    telefono:      [''],
    direccion:     [''],
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).+$/),
    ]],
  });

  private passwordValue = toSignal(
    this.form.controls['password'].valueChanges.pipe(startWith('')),
    { initialValue: '' }
  );

  passwordStrength = computed(() => {
    const p = this.passwordValue() ?? '';
    let score = 0;
    if (p.length >= 8) score++;
    if (/[A-Z]/.test(p)) score++;
    if (/[a-z]/.test(p)) score++;
    if (/\d/.test(p)) score++;
    if (/[^a-zA-Z\d]/.test(p)) score++;
    return score;
  });

  strengthLabel = computed(() => {
    const labels = ['', 'Muy débil', 'Débil', 'Regular', 'Fuerte', 'Muy fuerte'];
    const colors = ['', '#ef4444', '#f97316', '#eab308', '#22c55e', '#16a34a'];
    const s = this.passwordStrength();
    return { texto: labels[s] ?? '', color: colors[s] ?? '' };
  });

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.cargando.set(true);
    this.error.set(null);

    const v = this.form.value;
    this.authService.register({
      correo:        v.correo!,
      password:      v.password!,
      nombre:        v.nombre!,
      nombreUsuario: v.nombreUsuario || undefined,
      telefono:      v.telefono || undefined,
      ci:            v.ci || undefined,
      direccion:     v.direccion || undefined,
    }).subscribe({
      next: () => { this.cargando.set(false); this.exito.set(true); },
      error: err => {
        this.cargando.set(false);
        this.error.set(err.error?.mensaje ?? 'Error al registrarse');
      },
    });
  }
}
