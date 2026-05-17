import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { QrService } from '../../../core/services/qr.service';

@Component({
  selector: 'app-setup-2fa',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './setup-2fa.component.html',
  styleUrl: './setup-2fa.component.scss',
})
export class Setup2faComponent implements OnInit {
  private fb     = inject(FormBuilder);
  private auth   = inject(AuthService);
  private router = inject(Router);
  private qr     = inject(QrService);

  paso = signal<1 | 2>(1);
  qrDataUrl = signal<string | null>(null);
  secret    = signal<string | null>(null);
  cargando  = signal(false);
  error     = signal<string | null>(null);

  correo   = this.auth.pendingSetupCorreo;
  password = this.auth.pendingSetupPassword;

  codeForm = this.fb.group({
    codigo: [null as number | null, [Validators.required, Validators.min(100000), Validators.max(999999)]],
  });

  ngOnInit(): void {
    if (!this.correo() || !this.password()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.iniciarSetup();
  }

  private iniciarSetup(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.auth.init2faSetup(this.correo()!, this.password()!).subscribe({
      next: async resp => {
        this.secret.set(resp.secret);
        try {
          const dataUrl = await this.qr.toDataUrl(resp.qrUrl);
          this.qrDataUrl.set(dataUrl);
        } catch {
          this.error.set('Error al generar el código QR');
        }
        this.cargando.set(false);
        this.paso.set(2);
      },
      error: err => {
        this.cargando.set(false);
        this.error.set(err.error?.mensaje ?? 'Error al iniciar configuración 2FA');
      },
    });
  }

  onVerificar(): void {
    if (this.codeForm.invalid) return;
    this.cargando.set(true);
    this.error.set(null);

    const codigo = this.codeForm.value.codigo!;
    this.auth.enable2fa(this.correo()!, this.password()!, codigo).subscribe({
      next: () => {
        this.auth.pendingSetupCorreo.set(null);
        this.auth.pendingSetupPassword.set(null);
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.cargando.set(false);
        this.error.set(err.error?.mensaje ?? 'Código inválido. Inténtalo de nuevo.');
      },
    });
  }
}
