import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { startWith } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent implements OnInit {
  private fb    = inject(FormBuilder);
  private auth  = inject(AuthService);
  private route = inject(ActivatedRoute);

  cargando = signal(false);
  error    = signal<string | null>(null);
  exito    = signal(false);
  token    = signal<string | null>(null);
  mostrarPassword = signal(false);

  form = this.fb.group({
    nuevaPassword:    ['', [Validators.required, Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).+$/)]],
    confirmarPassword: ['', Validators.required],
  });

  get passCtrl()     { return this.form.get('nuevaPassword')!; }
  get confirmCtrl()  { return this.form.get('confirmarPassword')!; }

  private passValue = toSignal(
    this.form.controls['nuevaPassword'].valueChanges.pipe(startWith('')),
    { initialValue: '' }
  );

  passwordStrength = computed(() => {
    const p = this.passValue() ?? '';
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

  ngOnInit(): void {
    const t = this.route.snapshot.queryParamMap.get('token');
    this.token.set(t);
    if (!t) this.error.set('Token inválido o faltante. Solicita un nuevo enlace.');
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const { nuevaPassword, confirmarPassword } = this.form.value;
    if (nuevaPassword !== confirmarPassword) {
      this.error.set('Las contraseñas no coinciden');
      return;
    }
    this.cargando.set(true);
    this.error.set(null);

    this.auth.resetPassword(this.token()!, nuevaPassword!).subscribe({
      next: () => { this.cargando.set(false); this.exito.set(true); },
      error: err => {
        this.cargando.set(false);
        this.error.set(err.error?.mensaje ?? 'Error al restablecer la contraseña');
      },
    });
  }
}
