import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';
import {
  MascotasService,
  MascotaResponse,
  TemperamentoResponse,
  MascotaRequest,
  RestriccionRequest,
} from '../../core/services/mascotas.service';
import { ConfirmService } from '../../core/services/confirm.service';

interface ClienteItem {
  id: number;
  correo: string;
  nombre: string | null;
  ci: string | null;
  telefono: string | null;
  direccion: string | null;
  estado: string;
  mascotas: string[];
  preferencias?: Record<string, string>;
}

const TAMANOS = [
  { valor: 'PEQUEÑO',  label: 'Pequeño'  },
  { valor: 'MEDIANO',  label: 'Mediano'  },
  { valor: 'GRANDE',   label: 'Grande'   },
  { valor: 'GIGANTE',  label: 'Gigante'  },
];

const ESPECIES = [
  { valor: 'PERRO', label: 'Perro' },
  { valor: 'GATO',  label: 'Gato'  },
  { valor: 'OTRO',  label: 'Otro'  },
];

const TIPOS_RESTRICCION = [
  { valor: 'ALERGIA',          label: 'Alergia'           },
  { valor: 'CONDICION_MEDICA', label: 'Condición médica'  },
  { valor: 'COMPORTAMIENTO',   label: 'Comportamiento'    },
  { valor: 'OTRO',             label: 'Otro'              },
];

const GRAVEDADES = [
  { valor: 'LEVE',     label: 'Leve'     },
  { valor: 'MODERADA', label: 'Moderada' },
  { valor: 'GRAVE',    label: 'Grave'    },
];

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.scss',
})
export class ClientesComponent implements OnInit {
  private http        = inject(HttpClient);
  private auth        = inject(AuthService);
  private mascotasSvc = inject(MascotasService);
  private confirm     = inject(ConfirmService);
  private fb          = inject(FormBuilder);
  private apiUrl      = `${environment.apiUrl}/clientes`;

  isAdmin   = computed(() => this.auth.rol() === 'ADMIN');
  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
  isStaff   = computed(() => ['ADMIN','RECEPCION','GROOMER'].includes(this.auth.rol() ?? ''));

  // ── Staff state ─────────────────────────────────────────────
  clientes   = signal<ClienteItem[]>([]);
  cargando   = signal(false);
  error      = signal<string | null>(null);
  exito      = signal<string | null>(null);
  toggleando = signal<number | null>(null);

  // Staff: panel mascotas de un cliente
  staffPanel = signal<{ clienteId: number; clienteNombre: string; mascotas: MascotaResponse[] } | null>(null);
  loadingPanel = signal(false);

  // Staff: detalle de un cliente
  clienteDetalle = signal<ClienteItem | null>(null);

  // ── Cliente (Mis Mascotas) state ─────────────────────────────
  mascotas        = signal<MascotaResponse[]>([]);
  temperamentos   = signal<TemperamentoResponse[]>([]);
  loadingMascotas = signal(false);
  saving          = signal(false);
  errorM          = signal<string | null>(null);
  successM        = signal<string | null>(null);
  showModal       = signal(false);
  editingMascota  = signal<MascotaResponse | null>(null);
  detailMascota   = signal<MascotaResponse | null>(null);
  staffDetailMascota = signal<MascotaResponse | null>(null);

  // Archivos
  selectedFile     = signal<File | null>(null);
  selectedFileName = signal<string>('');
  selectedPhoto    = signal<File | null>(null);
  selectedPhotoName = signal<string>('');

  // Restricciones dinámicas
  restriccionesForm = signal<RestriccionRequest[]>([]);

  form!: FormGroup;

  tamanos         = TAMANOS;
  especies        = ESPECIES;
  tiposRestriccion = TIPOS_RESTRICCION;
  gravedades      = GRAVEDADES;

  ngOnInit(): void {
    if (this.isStaff()) {
      this.cargar();
    }
    if (this.isCliente()) {
      this.buildForm();
      this.cargarMascotas();
      this.cargarTemperamentos();
    }
  }

  // ── Staff: clientes ──────────────────────────────────────────

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.http.get<ClienteItem[]>(this.apiUrl).subscribe({
      next: data => { this.clientes.set(data); this.cargando.set(false); },
      error: () => { this.error.set('Error al cargar clientes'); this.cargando.set(false); },
    });
  }

  async toggleEstado(c: ClienteItem): Promise<void> {
    if (!this.isAdmin()) return;
    const accion = c.estado === 'ACTIVO' ? 'inhabilitar' : 'habilitar';
    const ok = await this.confirm.confirm({ title: `${accion.charAt(0).toUpperCase() + accion.slice(1)} cliente`, message: `¿Seguro que deseas ${accion} la cuenta de ${c.nombre ?? c.correo}?`, confirmLabel: accion.charAt(0).toUpperCase() + accion.slice(1), danger: c.estado === 'ACTIVO' });
    if (!ok) return;
    this.toggleando.set(c.id);
    this.error.set(null);
    this.exito.set(null);
    this.http.patch<ClienteItem>(`${this.apiUrl}/${c.id}/toggle`, {}).subscribe({
      next: actualizado => {
        this.clientes.update(list => list.map(x => x.id === actualizado.id ? actualizado : x));
        this.exito.set(`Cuenta ${actualizado.estado === 'ACTIVO' ? 'habilitada' : 'inhabilitada'} correctamente`);
        this.toggleando.set(null);
      },
      error: () => { this.error.set('Error al cambiar el estado'); this.toggleando.set(null); },
    });
  }

  verMascotas(clienteId: number, clienteNombre: string | null): void {
    this.loadingPanel.set(true);
    this.staffPanel.set({ clienteId, clienteNombre: clienteNombre ?? 'Cliente', mascotas: [] });
    this.mascotasSvc.mascotasByCliente(clienteId).subscribe({
      next: mascotas => {
        this.staffPanel.update(p => p ? { ...p, mascotas } : p);
        this.loadingPanel.set(false);
      },
      error: () => {
        this.loadingPanel.set(false);
        this.staffPanel.update(p => p ? { ...p, mascotas: [] } : p);
      },
    });
  }

  openClienteDetalle(c: ClienteItem): void { this.clienteDetalle.set(c); }
  closeClienteDetalle(): void { this.clienteDetalle.set(null); }

  closeStaffPanel(): void {
    this.staffPanel.set(null);
    this.staffDetailMascota.set(null);
  }

  openStaffDetail(m: MascotaResponse): void {
    this.staffDetailMascota.set(m);
  }

  closeStaffDetail(): void {
    this.staffDetailMascota.set(null);
  }

  // ── Cliente: mis mascotas ────────────────────────────────────

  private buildForm() {
    this.form = this.fb.group({
      nombre:          ['', Validators.required],
      especie:         ['', Validators.required],
      raza:            [''],
      tamano:          ['', Validators.required],
      fechaNacimiento: [''],
      alergias:        [''],
      temperamentoId:  [null],
    });
  }

  cargarMascotas() {
    this.loadingMascotas.set(true);
    this.mascotasSvc.misMascotas().subscribe({
      next: m  => { this.mascotas.set(m); this.loadingMascotas.set(false); },
      error: () => { this.loadingMascotas.set(false); },
    });
  }

  cargarTemperamentos() {
    this.mascotasSvc.temperamentos().subscribe({
      next: t => this.temperamentos.set(t),
    });
  }

  openModal(mascota?: MascotaResponse) {
    this.editingMascota.set(mascota ?? null);
    this.selectedFile.set(null);
    this.selectedFileName.set('');
    this.selectedPhoto.set(null);
    this.selectedPhotoName.set('');
    if (mascota) {
      this.form.patchValue({
        nombre:          mascota.nombre,
        especie:         mascota.especie,
        raza:            mascota.raza ?? '',
        tamano:          mascota.tamano,
        fechaNacimiento: mascota.fechaNacimiento ?? '',
        alergias:        mascota.alergias ?? '',
        temperamentoId:  mascota.temperamentoId ?? null,
      });
      this.restriccionesForm.set(
        (mascota.restricciones ?? []).map(r => ({
          tipo: r.tipo ?? '',
          descripcion: r.descripcion ?? '',
          gravedad: r.gravedad ?? 'LEVE',
        }))
      );
    } else {
      this.form.reset({ especie: '', tamano: '', temperamentoId: null });
      this.restriccionesForm.set([]);
    }
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.editingMascota.set(null);
  }

  openDetail(m: MascotaResponse): void {
    this.detailMascota.set(m);
  }

  closeDetail(): void {
    this.detailMascota.set(null);
  }

  editFromDetail(): void {
    const m = this.detailMascota();
    if (m) {
      this.detailMascota.set(null);
      this.openModal(m);
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedFile.set(input.files[0]);
      this.selectedFileName.set(input.files[0].name);
    }
  }

  onPhotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedPhoto.set(input.files[0]);
      this.selectedPhotoName.set(input.files[0].name);
    }
  }

  // Restricciones
  addRestriccion() {
    this.restriccionesForm.update(r => [...r, { tipo: 'ALERGIA', descripcion: '', gravedad: 'LEVE' }]);
  }

  removeRestriccion(i: number) {
    this.restriccionesForm.update(r => r.filter((_, idx) => idx !== i));
  }

  updateRestriccion(i: number, field: keyof RestriccionRequest, value: string) {
    this.restriccionesForm.update(r =>
      r.map((item, idx) => idx === i ? { ...item, [field]: value } : item)
    );
  }

  submitForm() {
    if (this.form.invalid) return;
    const v = this.form.value;
    const req: MascotaRequest = {
      nombre:          v.nombre,
      especie:         v.especie,
      raza:            v.raza || undefined,
      tamano:          v.tamano,
      fechaNacimiento: v.fechaNacimiento || undefined,
      alergias:        v.alergias || undefined,
      temperamentoId:  v.temperamentoId || null,
      restricciones:   this.restriccionesForm().filter(r => r.descripcion.trim()),
    };

    this.saving.set(true);
    const editing = this.editingMascota();
    const obs = editing
      ? this.mascotasSvc.actualizar(editing.id, req)
      : this.mascotasSvc.registrar(req);

    obs.subscribe({
      next: mascota => {
        const carnet = this.selectedFile();
        const foto   = this.selectedPhoto();

        const uploadNext = () => {
          if (foto) {
            this.mascotasSvc.subirFoto(mascota.id, foto).subscribe({
              next: () => this.afterSave(editing),
              error: () => {
                this.afterSave(editing);
                this.errorM.set('Mascota guardada pero la foto no pudo subirse');
              },
            });
          } else {
            this.afterSave(editing);
          }
        };

        if (carnet) {
          this.mascotasSvc.subirCarnet(mascota.id, carnet).subscribe({
            next: () => uploadNext(),
            error: () => {
              this.errorM.set('Mascota guardada pero el carnet no pudo subirse');
              uploadNext();
            },
          });
        } else {
          uploadNext();
        }
      },
      error: e => {
        this.errorM.set(e.error?.message ?? 'Error al guardar la mascota');
        this.saving.set(false);
      },
    });
  }

  private afterSave(editing: MascotaResponse | null) {
    this.saving.set(false);
    this.closeModal();
    this.cargarMascotas();
    this.showSuccessM(editing ? 'Mascota actualizada' : 'Mascota registrada');
  }

  async toggleMascota(m: MascotaResponse): Promise<void> {
    const accion = m.activa ? 'desactivar' : 'habilitar';
    const ok = await this.confirm.confirm({ title: `${accion.charAt(0).toUpperCase() + accion.slice(1)} mascota`, message: `¿Deseas ${accion} a ${m.nombre}?`, confirmLabel: accion.charAt(0).toUpperCase() + accion.slice(1), danger: m.activa });
    if (!ok) return;
    this.mascotasSvc.toggleActiva(m.id).subscribe({
      next: updated => {
        this.mascotas.update(list => list.map(x => x.id === updated.id ? updated : x));
        this.detailMascota.set(updated);
        this.showSuccessM(updated.activa ? `${updated.nombre} habilitada` : `${updated.nombre} desactivada`);
      },
      error: () => this.errorM.set('Error al cambiar el estado de la mascota'),
    });
  }

  // ── Helpers ──────────────────────────────────────────────────

  calcularEdad(fechaNacimiento?: string): string {
    if (!fechaNacimiento) return '';
    const hoy   = new Date();
    const nac   = new Date(fechaNacimiento);
    const years = hoy.getFullYear() - nac.getFullYear();
    const meses = hoy.getMonth() - nac.getMonth();
    if (years === 0) {
      const m = Math.max(0, meses);
      return `${m} mes${m !== 1 ? 'es' : ''}`;
    }
    return `${years} año${years !== 1 ? 's' : ''}`;
  }

  especieColor(especie: string): string {
    const map: Record<string, string> = { PERRO: '#fb923c', GATO: '#a78bfa', OTRO: '#5eead4' };
    return map[especie] ?? '#5eead4';
  }

  especieIcon(especie: string): string {
    return especie === 'PERRO' ? '🐕' : especie === 'GATO' ? '🐈' : '🐾';
  }

  tamanoLabel(t: string): string {
    return TAMANOS.find(x => x.valor === t)?.label ?? t;
  }

  tamanoClass(t: string): string {
    const map: Record<string, string> = {
      PEQUEÑO: 'tam-p', MEDIANO: 'tam-m', GRANDE: 'tam-g', GIGANTE: 'tam-x',
    };
    return map[t] ?? '';
  }

  gravedadClass(g: string): string {
    const map: Record<string, string> = {
      LEVE: 'grav-leve', MODERADA: 'grav-moderada', GRAVE: 'grav-grave',
    };
    return map[g] ?? 'grav-leve';
  }

  tipoLabel(t: string): string {
    return TIPOS_RESTRICCION.find(x => x.valor === t)?.label ?? t;
  }

  prefLabel(key: string): string {
    const map: Record<string, string> = {
      canal_preferido: 'Canal de contacto', idioma: 'Idioma',
      notificaciones: 'Notificaciones', recordatorio: 'Recordatorio',
    };
    return map[key] ?? key.replace(/_/g, ' ');
  }

  prefEntries(prefs?: Record<string, string>): { k: string; v: string }[] {
    if (!prefs) return [];
    return Object.entries(prefs).map(([k, v]) => ({ k, v }));
  }

  dismissErrorM() { this.errorM.set(null); }

  private showSuccessM(msg: string) {
    this.successM.set(msg);
    setTimeout(() => this.successM.set(null), 3000);
  }
}
