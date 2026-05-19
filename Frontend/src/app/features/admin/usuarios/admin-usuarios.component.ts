import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../environments/environment';

interface StaffResponse {
  id: number;
  correo: string;
  nombre: string | null;
  ci: string | null;
  telefono: string | null;
  rol: string;
  estado: string;
}

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './admin-usuarios.component.html',
  styleUrl: './admin-usuarios.component.scss',
})
export class AdminUsuariosComponent implements OnInit {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);
  private apiUrl = `${environment.apiUrl}/admin`;

  staff = signal<StaffResponse[]>([]);
  cargandoLista = signal(false);
  creando = signal(false);
  error = signal<string | null>(null);
  exito = signal<string | null>(null);


  editandoId      = signal<number | null>(null);
  miembroEditando = signal<StaffResponse | null>(null);
  guardandoEdicion = signal(false);
  errorEdicion = signal<string | null>(null);
  toggling = signal<number | null>(null);

  form = this.fb.group({
    correo:   ['', [Validators.required, Validators.email]],
    nombre:   ['', Validators.required],
    rol:      ['GROOMER', Validators.required],
    ci:       [''],
    telefono: [''],
  });

  editForm = this.fb.group({
    nombre:   ['', Validators.required],
    ci:       [''],
    telefono: [''],
  });

  ngOnInit(): void {
    this.cargarStaff();
  }

  cargarStaff(): void {
    this.cargandoLista.set(true);
    this.http.get<StaffResponse[]>(`${this.apiUrl}/staff`).subscribe({
      next: data => { this.staff.set(data); this.cargandoLista.set(false); },
      error: () => this.cargandoLista.set(false),
    });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.creando.set(true);
    this.error.set(null);
    this.exito.set(null);

    this.http.post<StaffResponse>(`${this.apiUrl}/staff`, this.form.value).subscribe({
      next: nuevo => {
        this.staff.update(s => [...s, nuevo]);
        this.exito.set(`Cuenta creada para ${nuevo.correo}. Se envió un correo con sus credenciales.`);
        this.form.reset({ rol: 'GROOMER' });
        this.creando.set(false);
      },
      error: err => {
        this.error.set(err.error?.message ?? 'Error al crear el usuario');
        this.creando.set(false);
      },
    });
  }

  iniciarEdicion(miembro: StaffResponse): void {
    this.editandoId.set(miembro.id);
    this.miembroEditando.set(miembro);
    this.errorEdicion.set(null);
    this.editForm.patchValue({ nombre: miembro.nombre ?? '', ci: miembro.ci ?? '', telefono: miembro.telefono ?? '' });
  }

  cancelarEdicion(): void {
    this.editandoId.set(null);
    this.miembroEditando.set(null);
    this.errorEdicion.set(null);
    this.editForm.reset();
  }

  guardarEdicion(): void {
    if (this.editForm.invalid) return;
    const id = this.editandoId();
    if (id === null) return;

    this.guardandoEdicion.set(true);
    this.errorEdicion.set(null);

    this.http.put<StaffResponse>(`${this.apiUrl}/staff/${id}`, this.editForm.value).subscribe({
      next: actualizado => {
        this.staff.update(s => s.map(m => m.id === id ? actualizado : m));
        this.guardandoEdicion.set(false);
        this.editandoId.set(null);
      },
      error: err => {
        this.errorEdicion.set(err.error?.message ?? 'Error al actualizar');
        this.guardandoEdicion.set(false);
      },
    });
  }

  toggleEstado(id: number): void {
    this.toggling.set(id);
    this.http.patch<StaffResponse>(`${this.apiUrl}/staff/${id}/toggle`, {}).subscribe({
      next: actualizado => {
        this.staff.update(s => s.map(m => m.id === id ? actualizado : m));
        this.toggling.set(null);
      },
      error: () => this.toggling.set(null),
    });
  }

  rolLabel(rol: string): string {
    const labels: Record<string, string> = { GROOMER: 'Groomer', RECEPCION: 'Recepción' };
    return labels[rol] ?? rol;
  }
}
