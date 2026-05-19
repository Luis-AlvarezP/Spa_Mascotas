import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { startWith } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { QrService } from '../../core/services/qr.service';
import { PerfilResponse, PreferenciaResponse } from '../../models/auth.model';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './perfil.component.html',
  styleUrl: './perfil.component.scss',
})
export class PerfilComponent implements OnInit {
  private fb    = inject(FormBuilder);
  private auth  = inject(AuthService);
  private route = inject(ActivatedRoute);
  private qr    = inject(QrService);

  perfil       = signal<PerfilResponse | null>(null);
  cargando     = signal(true);
  guardando    = signal(false);
  error        = signal<string | null>(null);
  exito        = signal<string | null>(null);
  onboarding   = signal(false);


  datoForm = this.fb.group({
    nombre:        [''],
    ci:            [''],
    telefono:      [''],
    nombreUsuario: [''],
    direccion:     [''],
  });

  guardandoPass     = signal(false);
  exitoPass         = signal<string | null>(null);
  errorPass         = signal<string | null>(null);
  mostrarPassActual = signal(false);
  mostrarPassNueva  = signal(false);

  changePassForm = this.fb.group({
    passwordActual:    ['', Validators.required],
    nuevaPassword:     ['', [Validators.required, Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).+$/)]],
    confirmarPassword: ['', Validators.required],
  });

  private passNuevaValue = toSignal(
    this.changePassForm.controls['nuevaPassword'].valueChanges.pipe(startWith('')),
    { initialValue: '' }
  );

  passwordStrength = computed(() => {
    const p = this.passNuevaValue() ?? '';
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

  canalPreferencia  = signal<PreferenciaResponse | null>(null);
  guardandoCanal    = signal(false);
  exitoCanal        = signal<string | null>(null);
  errorCanal        = signal<string | null>(null);

  canalForm = this.fb.group({
    canal: ['WhatsApp', Validators.required],
  });

  canales = ['WhatsApp', 'Correo electrónico'];

  preferencias      = signal<PreferenciaResponse[]>([]);
  cargandoPref      = signal(false);
  guardandoPref     = signal(false);
  eliminandoPref    = signal<number | null>(null);
  exitoPref         = signal<string | null>(null);
  errorPref         = signal<string | null>(null);

  prefForm = this.fb.group({
    nombre: ['', Validators.required],
    valor:  ['', Validators.required],
  });


  modoTotp     = signal<'idle' | 'setup' | 'disable'>('idle');
  totpQrUrl    = signal<string | null>(null);
  totpSecret   = signal<string | null>(null);
  totpCargando = signal(false);
  totpError    = signal<string | null>(null);

  codeForm = this.fb.group({
    codigo: [null as number | null, [Validators.required, Validators.min(100000), Validators.max(999999)]],
  });

  passwordForm = this.fb.group({
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    this.onboarding.set(this.route.snapshot.queryParamMap.get('onboarding') === 'true');
    this.cargarPerfil();
  }

  private cargarPerfil(): void {
    this.cargando.set(true);
    this.auth.getPerfil().subscribe({
      next: p => {
        this.perfil.set(p);
        this.datoForm.patchValue({
          nombre:        p.nombre ?? '',
          ci:            p.ci ?? '',
          telefono:      p.telefono ?? '',
          nombreUsuario: p.nombreUsuario ?? '',
          direccion:     p.direccion ?? '',
        });
        this.cargando.set(false);
        if (p.rol === 'CLIENTE') this.cargarPreferencias();
      },
      error: () => {
        this.error.set('Error al cargar el perfil');
        this.cargando.set(false);
      },
    });
  }

  guardarDatos(): void {
    this.guardando.set(true);
    this.error.set(null);
    this.exito.set(null);

    const { nombre, ci, telefono, nombreUsuario, direccion } = this.datoForm.value;
    this.auth.updatePerfil({
      nombre: nombre!,
      ci: ci ?? undefined,
      telefono: telefono ?? undefined,
      nombreUsuario: nombreUsuario ?? undefined,
      direccion: direccion ?? undefined,
    }).subscribe({
      next: p => {
        this.perfil.set(p);
        this.guardando.set(false);
        this.exito.set('Datos actualizados correctamente');
      },
      error: err => {
        this.guardando.set(false);
        this.error.set(err.error?.mensaje ?? 'Error al guardar');
      },
    });
  }


  cambiarPassword(): void {
    if (this.changePassForm.invalid) { this.changePassForm.markAllAsTouched(); return; }
    const { passwordActual, nuevaPassword, confirmarPassword } = this.changePassForm.value;
    if (nuevaPassword !== confirmarPassword) {
      this.errorPass.set('Las contraseñas no coinciden');
      return;
    }
    this.guardandoPass.set(true);
    this.exitoPass.set(null);
    this.errorPass.set(null);

    this.auth.changePassword(passwordActual!, nuevaPassword!).subscribe({
      next: () => {
        this.guardandoPass.set(false);
        this.exitoPass.set('Contraseña actualizada correctamente');
        this.changePassForm.reset();
      },
      error: err => {
        this.guardandoPass.set(false);
        this.errorPass.set(err.error?.mensaje ?? 'Error al cambiar la contraseña');
      },
    });
  }



  private cargarPreferencias(): void {
    this.cargandoPref.set(true);
    this.auth.getPreferencias().subscribe({
      next: data => {
        const canal = data.find(p => p.nombre === 'canal');
        this.canalPreferencia.set(canal ?? null);
        if (canal) this.canalForm.patchValue({ canal: canal.valor });
        this.preferencias.set(data.filter(p => p.nombre !== 'canal'));
        this.cargandoPref.set(false);
      },
      error: () => this.cargandoPref.set(false),
    });
  }

  guardarCanal(): void {
    if (this.canalForm.invalid) return;
    this.guardandoCanal.set(true);
    this.exitoCanal.set(null);
    this.errorCanal.set(null);

    const valor = this.canalForm.value.canal!;
    this.auth.savePreferencia('canal', valor).subscribe({
      next: pref => {
        this.canalPreferencia.set(pref);
        this.guardandoCanal.set(false);
        this.exitoCanal.set('Canal guardado');
      },
      error: err => {
        this.guardandoCanal.set(false);
        this.errorCanal.set(err.error?.mensaje ?? 'Error al guardar');
      },
    });
  }



  guardarPreferencia(): void {
    if (this.prefForm.invalid) return;
    this.guardandoPref.set(true);
    this.errorPref.set(null);
    const { nombre, valor } = this.prefForm.value;
    this.auth.savePreferencia(nombre!, valor!).subscribe({
      next: nueva => {
        this.preferencias.update(list => [...list.filter(p => p.nombre !== nombre), nueva]);
        this.prefForm.reset();
        this.guardandoPref.set(false);
        this.exitoPref.set('Preferencia guardada');
      },
      error: err => {
        this.guardandoPref.set(false);
        this.errorPref.set(err.error?.mensaje ?? 'Error al guardar preferencia');
      },
    });
  }

  eliminarPreferencia(id: number): void {
    this.eliminandoPref.set(id);
    this.auth.deletePreferencia(id).subscribe({
      next: () => {
        this.preferencias.update(list => list.filter(p => p.id !== id));
        this.eliminandoPref.set(null);
      },
      error: () => this.eliminandoPref.set(null),
    });
  }



  iniciarSetup2fa(): void {
    this.modoTotp.set('setup');
    this.totpCargando.set(true);
    this.totpError.set(null);

    this.auth.init2faLoggedIn().subscribe({
      next: async resp => {
        this.totpSecret.set(resp.secret);
        try {
          const dataUrl = await this.qr.toDataUrl(resp.qrUrl);
          this.totpQrUrl.set(dataUrl);
        } catch {
          this.totpError.set('Error al generar el código QR');
        }
        this.totpCargando.set(false);
      },
      error: err => {
        this.totpCargando.set(false);
        this.totpError.set(err.error?.mensaje ?? 'Error al iniciar 2FA');
        this.modoTotp.set('idle');
      },
    });
  }

  confirmar2fa(): void {
    if (this.codeForm.invalid) return;
    this.totpCargando.set(true);
    this.totpError.set(null);

    this.auth.enable2faLoggedIn(this.codeForm.value.codigo!).subscribe({
      next: () => {
        this.totpCargando.set(false);
        this.modoTotp.set('idle');
        this.totpQrUrl.set(null);
        this.totpSecret.set(null);
        this.exito.set('2FA activado correctamente');
        this.cargarPerfil();
      },
      error: err => {
        this.totpCargando.set(false);
        this.totpError.set(err.error?.mensaje ?? 'Código inválido');
      },
    });
  }

  iniciarDesactivar2fa(): void {
    this.modoTotp.set('disable');
    this.totpError.set(null);
  }

  desactivar2fa(): void {
    if (this.passwordForm.invalid) return;
    this.totpCargando.set(true);
    this.totpError.set(null);

    this.auth.disable2fa(this.passwordForm.value.password!).subscribe({
      next: () => {
        this.totpCargando.set(false);
        this.modoTotp.set('idle');
        this.passwordForm.reset();
        this.exito.set('2FA desactivado correctamente');
        this.cargarPerfil();
      },
      error: err => {
        this.totpCargando.set(false);
        this.totpError.set(err.error?.mensaje ?? 'Error al desactivar 2FA');
      },
    });
  }

  cancelarTotp(): void {
    this.modoTotp.set('idle');
    this.totpQrUrl.set(null);
    this.totpSecret.set(null);
    this.totpError.set(null);
    this.codeForm.reset();
    this.passwordForm.reset();
  }
}
