import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ConfirmService } from '../../core/services/confirm.service';
import {
  AgendaService,
  Bloqueo,
  BloqueoRequest,
  GroomerResponse,
  HorarioRequest,
  HorarioTrabajo,
} from '../../core/services/agenda.service';

const DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'];
const DIAS_LABELS: Record<string, string> = {
  LUNES: 'Lunes', MARTES: 'Martes', MIERCOLES: 'Miércoles',
  JUEVES: 'Jueves', VIERNES: 'Viernes', SABADO: 'Sábado', DOMINGO: 'Domingo',
};
const TIPOS_BLOQUEO = ['FERIADO', 'MANTENIMIENTO', 'AUSENCIA', 'DESCANSO', 'OTRO'];
const TIPOS_LABELS: Record<string, string> = {
  FERIADO: 'Feriado', MANTENIMIENTO: 'Mantenimiento',
  AUSENCIA: 'Ausencia', DESCANSO: 'Descanso', OTRO: 'Otro',
};

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.scss',
})
export class AgendaComponent implements OnInit {
  auth         = inject(AuthService);
  agendaSvc    = inject(AgendaService);
  confirm      = inject(ConfirmService);
  fb           = inject(FormBuilder);

  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
  isGroomer = computed(() => this.auth.rol() === 'GROOMER');
  isStaff   = computed(() => ['ADMIN', 'RECEPCION'].includes(this.auth.rol() ?? ''));

  // Navigation
  activeTab = signal<'horarios' | 'bloqueos'>('horarios');

  // Data
  groomers        = signal<GroomerResponse[]>([]);
  selectedGroomer = signal<GroomerResponse | null>(null);
  horarios        = signal<HorarioTrabajo[]>([]);
  bloqueos        = signal<Bloqueo[]>([]);

  // UI state
  loadingGroomers  = signal(false);
  loadingHorarios  = signal(false);
  loadingBloqueos  = signal(false);
  savingHorario    = signal(false);
  savingBloqueo    = signal(false);
  error            = signal<string | null>(null);
  success          = signal<string | null>(null);

  // Modal state
  showHorarioModal = signal(false);
  editingHorario   = signal<HorarioTrabajo | null>(null);
  showBloqueoModal = signal(false);
  editingBloqueo   = signal<Bloqueo | null>(null);

  // Forms
  horarioForm!: FormGroup;
  bloqueoForm!: FormGroup;

  // Constants for templates
  dias        = DIAS;
  diasLabels  = DIAS_LABELS;
  tiposBloqueo = TIPOS_BLOQUEO;
  tiposLabels = TIPOS_LABELS;

  today = new Date().toISOString().split('T')[0];

  horariosPorDia = computed(() =>
    DIAS.map(dia => ({
      dia,
      label: DIAS_LABELS[dia],
      items: this.horarios().filter(h => h.diaSemana === dia),
    }))
  );

  ngOnInit() {
    this.buildForms();
    if (this.isStaff()) {
      this.loadGroomers();
      this.loadBloqueos();
    }
  }

  private buildForms() {
    this.horarioForm = this.fb.group({
      diaSemana:       ['', Validators.required],
      horaInicio:      ['09:00', Validators.required],
      horaFin:         ['18:00', Validators.required],
      inicioAlmuerzo:  ['13:00'],
      finAlmuerzo:     ['14:00'],
      vigenteDesde:    [this.today, Validators.required],
      vigenteHasta:    [''],
      capacidadMaxima: [8, [Validators.required, Validators.min(1), Validators.max(20)]],
    });

    this.bloqueoForm = this.fb.group({
      titulo:      ['', Validators.required],
      tipo:        ['FERIADO', Validators.required],
      fechaInicio: ['', Validators.required],
      fechaFin:    ['', Validators.required],
      empleadoId:  [null],
      descripcion: [''],
    });
  }

  // ── Groomers ────────────────────────────────────────────────

  loadGroomers() {
    this.loadingGroomers.set(true);
    this.agendaSvc.groomers().subscribe({
      next: g  => { this.groomers.set(g); this.loadingGroomers.set(false); },
      error: () => { this.error.set('Error cargando groomers'); this.loadingGroomers.set(false); },
    });
  }

  selectGroomer(groomer: GroomerResponse) {
    this.selectedGroomer.set(groomer);
    this.loadHorarios(groomer.id);
  }

  // ── Horarios ────────────────────────────────────────────────

  loadHorarios(empleadoId: number) {
    this.loadingHorarios.set(true);
    this.agendaSvc.horariosPorGroomer(empleadoId).subscribe({
      next: h  => { this.horarios.set(h); this.loadingHorarios.set(false); },
      error: () => { this.loadingHorarios.set(false); },
    });
  }

  openHorarioModal(horario?: HorarioTrabajo) {
    this.editingHorario.set(horario ?? null);
    if (horario) {
      this.horarioForm.patchValue({
        diaSemana:       horario.diaSemana,
        horaInicio:      horario.horaInicio.substring(0, 5),
        horaFin:         horario.horaFin.substring(0, 5),
        inicioAlmuerzo:  horario.inicioAlmuerzo ? horario.inicioAlmuerzo.substring(0, 5) : '',
        finAlmuerzo:     horario.finAlmuerzo    ? horario.finAlmuerzo.substring(0, 5)    : '',
        vigenteDesde:    horario.vigenteDesde,
        vigenteHasta:    horario.vigenteHasta ?? '',
        capacidadMaxima: horario.capacidadMaxima,
      });
    } else {
      this.horarioForm.reset({
        horaInicio: '09:00', horaFin: '18:00',
        inicioAlmuerzo: '13:00', finAlmuerzo: '14:00',
        vigenteDesde: this.today, capacidadMaxima: 8,
      });
    }
    this.showHorarioModal.set(true);
  }

  closeHorarioModal() {
    this.showHorarioModal.set(false);
    this.editingHorario.set(null);
  }

  submitHorario() {
    if (this.horarioForm.invalid || !this.selectedGroomer()) return;
    const v = this.horarioForm.value;
    const req: HorarioRequest = {
      empleadoId:      this.selectedGroomer()!.id,
      diaSemana:       v.diaSemana,
      horaInicio:      v.horaInicio + ':00',
      horaFin:         v.horaFin    + ':00',
      inicioAlmuerzo:  v.inicioAlmuerzo ? v.inicioAlmuerzo + ':00' : undefined,
      finAlmuerzo:     v.finAlmuerzo    ? v.finAlmuerzo    + ':00' : undefined,
      vigenteDesde:    v.vigenteDesde,
      vigenteHasta:    v.vigenteHasta   || undefined,
      capacidadMaxima: v.capacidadMaxima,
    };

    this.savingHorario.set(true);
    const editing = this.editingHorario();
    const obs = editing
      ? this.agendaSvc.actualizarHorario(editing.id, req)
      : this.agendaSvc.crearHorario(req);

    obs.subscribe({
      next: () => {
        this.closeHorarioModal();
        this.loadHorarios(this.selectedGroomer()!.id);
        this.showSuccess(editing ? 'Horario actualizado' : 'Horario creado');
        this.savingHorario.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Error al guardar horario');
        this.savingHorario.set(false);
      },
    });
  }

  async deleteHorario(id: number) {
    const ok = await this.confirm.confirm({ title: 'Eliminar horario', message: '¿Seguro que deseas eliminar este horario de trabajo?', confirmLabel: 'Eliminar', danger: true });
    if (!ok) return;
    this.agendaSvc.eliminarHorario(id).subscribe({
      next: () => { this.loadHorarios(this.selectedGroomer()!.id); this.showSuccess('Horario eliminado'); },
      error: () => this.error.set('Error al eliminar el horario'),
    });
  }

  // ── Bloqueos ────────────────────────────────────────────────

  loadBloqueos() {
    this.loadingBloqueos.set(true);
    this.agendaSvc.bloqueos().subscribe({
      next: b  => { this.bloqueos.set(b); this.loadingBloqueos.set(false); },
      error: () => { this.loadingBloqueos.set(false); },
    });
  }

  openBloqueoModal(bloqueo?: Bloqueo) {
    this.editingBloqueo.set(bloqueo ?? null);
    if (bloqueo) {
      this.bloqueoForm.patchValue({
        titulo:      bloqueo.titulo,
        tipo:        bloqueo.tipo,
        fechaInicio: bloqueo.fechaInicio.substring(0, 16),
        fechaFin:    bloqueo.fechaFin.substring(0, 16),
        empleadoId:  bloqueo.empleadoId ?? null,
        descripcion: bloqueo.descripcion ?? '',
      });
    } else {
      this.bloqueoForm.reset({ tipo: 'FERIADO' });
    }
    this.showBloqueoModal.set(true);
  }

  closeBloqueoModal() {
    this.showBloqueoModal.set(false);
    this.editingBloqueo.set(null);
  }

  submitBloqueo() {
    if (this.bloqueoForm.invalid) return;
    const v = this.bloqueoForm.value;
    const req: BloqueoRequest = {
      titulo:      v.titulo,
      tipo:        v.tipo,
      fechaInicio: v.fechaInicio + ':00',
      fechaFin:    v.fechaFin    + ':00',
      empleadoId:  v.empleadoId  || undefined,
      descripcion: v.descripcion,
    };

    this.savingBloqueo.set(true);
    const editing = this.editingBloqueo();
    const obs = editing
      ? this.agendaSvc.actualizarBloqueo(editing.id, req)
      : this.agendaSvc.crearBloqueo(req);

    obs.subscribe({
      next: () => {
        this.closeBloqueoModal();
        this.loadBloqueos();
        this.showSuccess(editing ? 'Bloqueo actualizado' : 'Bloqueo creado');
        this.savingBloqueo.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Error al guardar bloqueo');
        this.savingBloqueo.set(false);
      },
    });
  }

  async deleteBloqueo(id: number) {
    const ok = await this.confirm.confirm({ title: 'Eliminar bloqueo', message: '¿Seguro que deseas eliminar este bloqueo de agenda?', confirmLabel: 'Eliminar', danger: true });
    if (!ok) return;
    this.agendaSvc.eliminarBloqueo(id).subscribe({
      next: () => { this.loadBloqueos(); this.showSuccess('Bloqueo eliminado'); },
      error: () => this.error.set('Error al eliminar el bloqueo'),
    });
  }

  // ── Helpers ─────────────────────────────────────────────────

  private showSuccess(msg: string) {
    this.success.set(msg);
    setTimeout(() => this.success.set(null), 3000);
  }

  formatTime(t: string): string { return t ? t.substring(0, 5) : '—'; }

  formatDateTime(dt: string): string {
    if (!dt) return '—';
    const d = new Date(dt);
    return d.toLocaleDateString('es-BO', { day: '2-digit', month: 'short', year: 'numeric' })
      + ' ' + d.toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit' });
  }

  tipoLabel(tipo: string): string { return TIPOS_LABELS[tipo] ?? tipo; }

  dismissError() { this.error.set(null); }
}
