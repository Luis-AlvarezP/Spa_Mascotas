import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import jsPDF from 'jspdf';
import { AuthService } from '../../core/services/auth.service';
import { ConfirmService } from '../../core/services/confirm.service';
import {
  AgendaService, Bloqueo, BloqueoRequest,
  GroomerResponse, HorarioRequest, HorarioTrabajo,
} from '../../core/services/agenda.service';
import { CitaService, CitaResponse, ServicioResponse, SlotResponse, GroomerBasicResponse } from '../../core/services/cita.service';
import { CitaNotificacionService } from '../../core/services/cita-notificacion.service';
import { MascotasService, MascotaResponse } from '../../core/services/mascotas.service';

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
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.scss',
})
export class AgendaComponent implements OnInit {
  auth        = inject(AuthService);
  agendaSvc   = inject(AgendaService);
  citaSvc     = inject(CitaService);
  citaNotif   = inject(CitaNotificacionService);
  mascotasSvc = inject(MascotasService);
  confirm     = inject(ConfirmService);
  fb          = inject(FormBuilder);

  citasBadge = computed(() => this.citaNotif.enRevision() + this.citaNotif.pendientePago());

  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
  isGroomer = computed(() => this.auth.rol() === 'GROOMER');
  isStaff   = computed(() => ['ADMIN', 'RECEPCION'].includes(this.auth.rol() ?? ''));

  // ── Staff ────────────────────────────────────────────────
  activeTab = signal<'horarios' | 'bloqueos' | 'citas'>('horarios');

  groomers        = signal<GroomerResponse[]>([]);
  selectedGroomer = signal<GroomerResponse | null>(null);
  horarios        = signal<HorarioTrabajo[]>([]);
  bloqueos        = signal<Bloqueo[]>([]);

  loadingGroomers = signal(false);
  loadingHorarios = signal(false);
  loadingBloqueos = signal(false);
  savingHorario   = signal(false);
  savingBloqueo   = signal(false);

  showHorarioModal = signal(false);
  editingHorario   = signal<HorarioTrabajo | null>(null);
  showBloqueoModal = signal(false);
  editingBloqueo   = signal<Bloqueo | null>(null);

  horarioForm!: FormGroup;
  bloqueoForm!: FormGroup;

  dias         = DIAS;
  diasLabels   = DIAS_LABELS;
  tiposBloqueo = TIPOS_BLOQUEO;
  tiposLabels  = TIPOS_LABELS;
  today        = new Date().toISOString().split('T')[0];

  horariosPorDia = computed(() =>
    DIAS.map(dia => ({
      dia, label: DIAS_LABELS[dia],
      items: this.horarios().filter(h => h.diaSemana === dia),
    }))
  );

  // ── Cliente ──────────────────────────────────────────────
  misCitas            = signal<CitaResponse[]>([]);
  servicios           = signal<ServicioResponse[]>([]);
  misMascotas         = signal<MascotaResponse[]>([]);
  groomersCliente     = signal<GroomerBasicResponse[]>([]);
  slots               = signal<SlotResponse[]>([]);
  todasCitas          = signal<CitaResponse[]>([]);
  misServciosGroomer  = signal<CitaResponse[]>([]);
  loadingTodasCitas   = signal(false);
  loadingMisServicios = signal(false);
  savingCitaStaff     = signal<number | null>(null);

  showCobrarModal  = signal(false);
  showRechazarModal = signal(false);
  citaEnAccion     = signal<CitaResponse | null>(null);
  metodoPago       = signal<string>('EFECTIVO');
  motivoRechazo    = signal<string>('');
  savingCobro      = signal(false);
  savingRechazo    = signal(false);

  loadingCitas        = signal(false);
  loadingSlots        = signal(false);
  savingCita          = signal(false);
  savingCancelar      = signal(false);
  savingReprog        = signal(false);

  showSolicitarModal  = signal(false);
  showCancelarModal   = signal(false);
  showReprogModal     = signal(false);
  citaSeleccionada    = signal<CitaResponse | null>(null);

  mascotaActual          = signal<MascotaResponse | null>(null);
  servicioActual         = signal<ServicioResponse | null>(null);
  slotSelSol             = signal<SlotResponse | null>(null);
  slotSelReprog          = signal<SlotResponse | null>(null);
  groomerDelSlotSolId    = signal<number | null>(null);
  groomerDelSlotReprogId = signal<number | null>(null);

  precioEstimado   = signal<number | null>(null);
  duracionEstimada = signal<number | null>(null);

  slotsDisponibles = computed(() => this.slots().filter(s => s.disponible));
  citasVisibles    = computed(() => this.misCitas().filter(c => c.estado !== 'CANCELADO'));
  penalizacionActual = computed(() => {
    const todas = this.misCitas();
    return todas.length > 0 ? (todas[0].penalizacionCliente ?? 0) : 0;
  });
  precioConPenalizacion = computed(() => {
    const base = this.precioEstimado();
    const pen  = this.penalizacionActual();
    if (base === null) return null;
    return pen > 0 ? +(base + base * pen / 100).toFixed(2) : base;
  });

  solicitarForm!: FormGroup;
  cancelarForm!:  FormGroup;
  reprogForm!:    FormGroup;

  error   = signal<string | null>(null);
  success = signal<string | null>(null);

  // ── Init ─────────────────────────────────────────────────

  ngOnInit() {
    this.buildForms();
    if (this.isStaff()) {
      this.loadGroomers();
      this.loadBloqueos();
      this.loadTodasCitas();
    } else if (this.isGroomer()) {
      this.loadMisServicios();
    } else if (this.isCliente()) {
      this.loadMisCitas();
      this.citaSvc.servicios().subscribe({ next: s => this.servicios.set(s) });
      this.mascotasSvc.misMascotas().subscribe({ next: m => this.misMascotas.set(m) });
      this.citaSvc.groomers().subscribe({ next: g => this.groomersCliente.set(g) });
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
    this.solicitarForm = this.fb.group({
      mascotaId:          [null, Validators.required],
      servicioId:         [null, Validators.required],
      empleadoPreferidoId:[''],
      fecha:              ['', Validators.required],
      slot:               ['', Validators.required],
      notas:              [''],
    });
    this.cancelarForm = this.fb.group({
      motivo: ['', Validators.required],
    });
    this.reprogForm = this.fb.group({
      empleadoPreferidoId: [''],
      fecha: ['', Validators.required],
      slot:  ['', Validators.required],
      notas: [''],
    });
  }

  // ── Staff: horarios ──────────────────────────────────────

  loadGroomers() {
    this.loadingGroomers.set(true);
    this.agendaSvc.groomers().subscribe({
      next: g  => { this.groomers.set(g); this.loadingGroomers.set(false); },
      error: () => { this.error.set('Error cargando groomers'); this.loadingGroomers.set(false); },
    });
  }

  selectGroomer(g: GroomerResponse) {
    this.selectedGroomer.set(g);
    this.loadHorarios(g.id);
  }

  loadHorarios(empleadoId: number) {
    this.loadingHorarios.set(true);
    this.agendaSvc.horariosPorGroomer(empleadoId).subscribe({
      next: h  => { this.horarios.set(h); this.loadingHorarios.set(false); },
      error: () => this.loadingHorarios.set(false),
    });
  }

  openHorarioModal(horario?: HorarioTrabajo) {
    this.editingHorario.set(horario ?? null);
    if (horario) {
      this.horarioForm.patchValue({
        diaSemana: horario.diaSemana,
        horaInicio: horario.horaInicio.substring(0, 5),
        horaFin: horario.horaFin.substring(0, 5),
        inicioAlmuerzo: horario.inicioAlmuerzo ? horario.inicioAlmuerzo.substring(0, 5) : '',
        finAlmuerzo: horario.finAlmuerzo ? horario.finAlmuerzo.substring(0, 5) : '',
        vigenteDesde: horario.vigenteDesde,
        vigenteHasta: horario.vigenteHasta ?? '',
        capacidadMaxima: horario.capacidadMaxima,
      });
    } else {
      this.horarioForm.reset({ horaInicio: '09:00', horaFin: '18:00', inicioAlmuerzo: '13:00', finAlmuerzo: '14:00', vigenteDesde: this.today, capacidadMaxima: 8 });
    }
    this.showHorarioModal.set(true);
  }

  closeHorarioModal() { this.showHorarioModal.set(false); this.editingHorario.set(null); }

  submitHorario() {
    if (this.horarioForm.invalid || !this.selectedGroomer()) return;
    const v = this.horarioForm.value;
    const req: HorarioRequest = {
      empleadoId: this.selectedGroomer()!.id, diaSemana: v.diaSemana,
      horaInicio: v.horaInicio + ':00', horaFin: v.horaFin + ':00',
      inicioAlmuerzo: v.inicioAlmuerzo ? v.inicioAlmuerzo + ':00' : undefined,
      finAlmuerzo: v.finAlmuerzo ? v.finAlmuerzo + ':00' : undefined,
      vigenteDesde: v.vigenteDesde, vigenteHasta: v.vigenteHasta || undefined,
      capacidadMaxima: v.capacidadMaxima,
    };
    this.savingHorario.set(true);
    const editing = this.editingHorario();
    const obs = editing ? this.agendaSvc.actualizarHorario(editing.id, req) : this.agendaSvc.crearHorario(req);
    obs.subscribe({
      next: () => { this.closeHorarioModal(); this.loadHorarios(this.selectedGroomer()!.id); this.showSuccess(editing ? 'Horario actualizado' : 'Horario creado'); this.savingHorario.set(false); },
      error: e => { this.error.set(e.error?.message ?? 'Error al guardar'); this.savingHorario.set(false); },
    });
  }

  async deleteHorario(id: number) {
    const ok = await this.confirm.confirm({ title: 'Eliminar horario', message: '¿Eliminar este horario de trabajo?', confirmLabel: 'Eliminar', danger: true });
    if (!ok) return;
    this.agendaSvc.eliminarHorario(id).subscribe({
      next: () => { this.loadHorarios(this.selectedGroomer()!.id); this.showSuccess('Horario eliminado'); },
      error: () => this.error.set('Error al eliminar'),
    });
  }

  // ── Staff: bloqueos ──────────────────────────────────────

  loadBloqueos() {
    this.loadingBloqueos.set(true);
    this.agendaSvc.bloqueos().subscribe({
      next: b  => { this.bloqueos.set(b); this.loadingBloqueos.set(false); },
      error: () => this.loadingBloqueos.set(false),
    });
  }

  openBloqueoModal(bloqueo?: Bloqueo) {
    this.editingBloqueo.set(bloqueo ?? null);
    if (bloqueo) {
      this.bloqueoForm.patchValue({
        titulo: bloqueo.titulo, tipo: bloqueo.tipo,
        fechaInicio: bloqueo.fechaInicio.substring(0, 16), fechaFin: bloqueo.fechaFin.substring(0, 16),
        empleadoId: bloqueo.empleadoId ?? null, descripcion: bloqueo.descripcion ?? '',
      });
    } else {
      this.bloqueoForm.reset({ tipo: 'FERIADO' });
    }
    this.showBloqueoModal.set(true);
  }

  closeBloqueoModal() { this.showBloqueoModal.set(false); this.editingBloqueo.set(null); }

  submitBloqueo() {
    if (this.bloqueoForm.invalid) return;
    const v = this.bloqueoForm.value;
    const req: BloqueoRequest = {
      titulo: v.titulo, tipo: v.tipo,
      fechaInicio: v.fechaInicio + ':00', fechaFin: v.fechaFin + ':00',
      empleadoId: v.empleadoId || undefined, descripcion: v.descripcion,
    };
    this.savingBloqueo.set(true);
    const editing = this.editingBloqueo();
    const obs = editing ? this.agendaSvc.actualizarBloqueo(editing.id, req) : this.agendaSvc.crearBloqueo(req);
    obs.subscribe({
      next: () => { this.closeBloqueoModal(); this.loadBloqueos(); this.showSuccess(editing ? 'Bloqueo actualizado' : 'Bloqueo creado'); this.savingBloqueo.set(false); },
      error: e => { this.error.set(e.error?.message ?? 'Error'); this.savingBloqueo.set(false); },
    });
  }

  async deleteBloqueo(id: number) {
    const ok = await this.confirm.confirm({ title: 'Eliminar bloqueo', message: '¿Eliminar este bloqueo?', confirmLabel: 'Eliminar', danger: true });
    if (!ok) return;
    this.agendaSvc.eliminarBloqueo(id).subscribe({
      next: () => { this.loadBloqueos(); this.showSuccess('Bloqueo eliminado'); },
      error: () => this.error.set('Error al eliminar'),
    });
  }

  // ── Staff: citas ─────────────────────────────────────────

  loadTodasCitas() {
    this.loadingTodasCitas.set(true);
    this.citaSvc.todasLasCitas().subscribe({
      next: c  => { this.todasCitas.set(c); this.loadingTodasCitas.set(false); },
      error: () => this.loadingTodasCitas.set(false),
    });
  }

  loadMisServicios() {
    this.loadingMisServicios.set(true);
    this.citaSvc.misServicios().subscribe({
      next: c  => { this.misServciosGroomer.set(c); this.loadingMisServicios.set(false); },
      error: () => this.loadingMisServicios.set(false),
    });
  }

  aceptarCita(id: number) {
    this.savingCitaStaff.set(id);
    this.citaSvc.aceptar(id).subscribe({
      next: () => { this.loadTodasCitas(); this.showSuccess('Cita aceptada'); this.savingCitaStaff.set(null); },
      error: e  => { this.error.set(e.error?.message ?? 'Error al aceptar'); this.savingCitaStaff.set(null); },
    });
  }

  finalizarCita(id: number) {
    this.savingCitaStaff.set(id);
    this.citaSvc.finalizarServicio(id).subscribe({
      next: () => { this.loadMisServicios(); this.showSuccess('Servicio marcado como terminado'); this.savingCitaStaff.set(null); },
      error: e  => { this.error.set(e.error?.message ?? 'Error'); this.savingCitaStaff.set(null); },
    });
  }

  openCobrar(c: CitaResponse) {
    this.citaEnAccion.set(c);
    this.metodoPago.set('EFECTIVO');
    this.showCobrarModal.set(true);
  }

  closeCobrar() { this.showCobrarModal.set(false); this.citaEnAccion.set(null); }

  submitCobrar() {
    const cita = this.citaEnAccion();
    if (!cita || this.savingCobro()) return;
    this.savingCobro.set(true);
    this.citaSvc.cobrar(cita.id, this.metodoPago()).subscribe({
      next: c => {
        this.closeCobrar();
        this.loadTodasCitas();
        this.showSuccess('Pago confirmado, cita realizada');
        this.savingCobro.set(false);
        this.descargarReciboCita(c);
      },
      error: e => { this.error.set(e.error?.message ?? 'Error al cobrar'); this.savingCobro.set(false); },
    });
  }

  cobrarCita(id: number) {
    const cita = this.todasCitas().find(c => c.id === id) ?? null;
    if (cita) this.openCobrar(cita);
  }

  openRechazar(c: CitaResponse) {
    this.citaEnAccion.set(c);
    this.motivoRechazo.set('');
    this.showRechazarModal.set(true);
  }

  closeRechazar() { this.showRechazarModal.set(false); this.citaEnAccion.set(null); }

  submitRechazar() {
    const cita = this.citaEnAccion();
    const motivo = this.motivoRechazo().trim();
    if (!cita || !motivo || this.savingRechazo()) return;
    this.savingRechazo.set(true);
    this.citaSvc.rechazar(cita.id, motivo).subscribe({
      next: () => {
        this.closeRechazar();
        this.loadTodasCitas();
        this.showSuccess('Cita rechazada');
        this.savingRechazo.set(false);
      },
      error: e => { this.error.set(e.error?.message ?? 'Error al rechazar'); this.savingRechazo.set(false); },
    });
  }

  descargarReciboCita(c: CitaResponse) {
    const doc = new jsPDF({ format: 'a5', unit: 'mm' });
    const W = doc.internal.pageSize.getWidth();

    doc.setFillColor(18, 14, 35);
    doc.rect(0, 0, W, doc.internal.pageSize.getHeight(), 'F');

    doc.setFontSize(18); doc.setTextColor(196, 181, 253);
    doc.setFont('helvetica', 'bold');
    doc.text('SpaMascotas', W / 2, 18, { align: 'center' });

    doc.setFontSize(10); doc.setTextColor(148, 163, 184);
    doc.setFont('helvetica', 'normal');
    doc.text('Recibo de servicio de grooming', W / 2, 25, { align: 'center' });

    doc.setDrawColor(124, 58, 237); doc.setLineWidth(0.4);
    doc.line(10, 30, W - 10, 30);

    const rows: [string, string][] = [
      ['N° Cita',    `#${c.id}`],
      ['Cliente',    c.clienteNombre ?? '—'],
      ['CI',         c.clienteCi ?? '—'],
      ['Mascota',    c.mascotaNombre],
      ['Servicio',   c.servicioNombre],
      ['Duración',   `${c.duracionMinutos} min`],
      ['Groomer',    c.empleadoAsignadoNombre ?? '—'],
      ['Fecha',      this.formatDT(c.fechaHoraInicio)],
      ['Método pago', c.metodoPago ?? this.metodoPago()],
    ];

    let y = 38;
    doc.setFontSize(9);
    for (const [label, val] of rows) {
      doc.setTextColor(148, 163, 184); doc.setFont('helvetica', 'normal');
      doc.text(label, 14, y);
      doc.setTextColor(226, 232, 240); doc.setFont('helvetica', 'bold');
      doc.text(val, 80, y);
      y += 7;
    }

    doc.setDrawColor(124, 58, 237); doc.line(10, y, W - 10, y); y += 7;

    if (c.recargoPorcentaje > 0) {
      doc.setFontSize(9); doc.setTextColor(148, 163, 184); doc.setFont('helvetica', 'normal');
      doc.text('Precio base:', 14, y);
      doc.setTextColor(226, 232, 240);
      doc.text(`Bs. ${c.precioFinal.toFixed(2)}`, 80, y); y += 7;

      doc.setTextColor(251, 191, 36); doc.setFont('helvetica', 'normal');
      doc.text(`Recargo (+${c.recargoPorcentaje}%):`, 14, y);
      const recargo = +(c.precioFinal * c.recargoPorcentaje / 100).toFixed(2);
      doc.text(`+Bs. ${recargo.toFixed(2)}`, 80, y); y += 7;
    }

    doc.setFontSize(12); doc.setTextColor(167, 139, 250); doc.setFont('helvetica', 'bold');
    doc.text('TOTAL:', 14, y);
    doc.text(`Bs. ${this.precioFinalConRecargo(c).toFixed(2)}`, 80, y); y += 12;

    doc.setFontSize(8); doc.setTextColor(100, 116, 139); doc.setFont('helvetica', 'italic');
    doc.text('Gracias por confiar en SpaMascotas', W / 2, y, { align: 'center' });

    doc.save(`recibo-cita-${c.id}.pdf`);
  }

  // ── Cliente: solicitar ───────────────────────────────────

  loadMisCitas() {
    this.loadingCitas.set(true);
    this.citaSvc.misCitas().subscribe({
      next: c => { this.misCitas.set(c); this.loadingCitas.set(false); },
      error: () => this.loadingCitas.set(false),
    });
  }

  openSolicitar() {
    this.solicitarForm.reset({ empleadoPreferidoId: '' });
    this.slots.set([]);
    this.precioEstimado.set(null);
    this.duracionEstimada.set(null);
    this.mascotaActual.set(null);
    this.servicioActual.set(null);
    this.slotSelSol.set(null);
    this.groomerDelSlotSolId.set(null);
    this.showSolicitarModal.set(true);
  }

  closeSolicitar() { this.showSolicitarModal.set(false); }

  selectMascota(m: MascotaResponse) {
    this.solicitarForm.patchValue({ mascotaId: m.id });
    this.mascotaActual.set(m);
    this.onServicioOMascotaChange();
  }

  onServicioOMascotaChange() {
    const v = this.solicitarForm.value;
    const srv = this.servicios().find(s => s.id === +v.servicioId);
    const m   = this.mascotaActual() ?? this.misMascotas().find(p => p.id === +v.mascotaId) ?? null;
    this.servicioActual.set(srv ?? null);
    if (!srv || !m) { this.precioEstimado.set(null); this.duracionEstimada.set(null); this.slots.set([]); return; }
    this.mascotaActual.set(m);
    this.precioEstimado.set(this.citaSvc.calcularPrecio(srv.precioBase, m.tamano ?? '', m.temperamentoNombre ?? null));
    this.duracionEstimada.set(this.citaSvc.calcularDuracion(srv.duracionMinutos, m.tamano ?? '', m.temperamentoNombre ?? null));
    if (v.fecha) {
      const empleadoId = v.empleadoPreferidoId ? +v.empleadoPreferidoId : undefined;
      this.cargarSlots(v.fecha, +v.servicioId, +v.mascotaId, empleadoId);
    }
  }

  onFechaChange() {
    const v = this.solicitarForm.value;
    if (!v.fecha || !v.servicioId || !v.mascotaId) return;
    const empleadoId = v.empleadoPreferidoId ? +v.empleadoPreferidoId : undefined;
    this.cargarSlots(v.fecha, +v.servicioId, +v.mascotaId, empleadoId);
  }

  onGroomerChange() {
    const v = this.solicitarForm.value;
    this.slotSelSol.set(null);
    this.groomerDelSlotSolId.set(null);
    if (!v.fecha || !v.servicioId || !v.mascotaId) return;
    const empleadoId = v.empleadoPreferidoId ? +v.empleadoPreferidoId : undefined;
    this.cargarSlots(v.fecha, +v.servicioId, +v.mascotaId, empleadoId);
  }

  private cargarSlots(fecha: string, servicioId: number, mascotaId: number, empleadoId?: number) {
    this.loadingSlots.set(true);
    this.solicitarForm.patchValue({ slot: '' });
    this.slotSelSol.set(null);
    this.groomerDelSlotSolId.set(null);
    this.citaSvc.slots(fecha, servicioId, mascotaId, empleadoId).subscribe({
      next: s => { this.slots.set(s); this.loadingSlots.set(false); },
      error: () => this.loadingSlots.set(false),
    });
  }

  submitSolicitar() {
    if (this.solicitarForm.invalid || this.savingCita() || this.needsGroomerSol()) return;
    const v = this.solicitarForm.value;
    this.savingCita.set(true);
    this.citaSvc.solicitar({
      mascotaId: +v.mascotaId, servicioId: +v.servicioId,
      empleadoPreferidoId: this.groomerDelSlotSolId() ?? undefined,
      fechaHoraInicio: v.slot,
      notas: v.notas || undefined,
    }).subscribe({
      next: () => { this.closeSolicitar(); this.loadMisCitas(); this.showSuccess('Cita solicitada, queda en revisión'); this.savingCita.set(false); },
      error: e  => { this.error.set(e.error?.message ?? 'Error al solicitar'); this.savingCita.set(false); },
    });
  }

  // ── Cliente: cancelar ────────────────────────────────────

  openCancelar(cita: CitaResponse) {
    this.citaSeleccionada.set(cita);
    this.cancelarForm.reset();
    this.showCancelarModal.set(true);
  }

  closeCancelar() { this.showCancelarModal.set(false); this.citaSeleccionada.set(null); }

  submitCancelar() {
    if (this.cancelarForm.invalid || this.savingCancelar()) return;
    const id = this.citaSeleccionada()!.id;
    this.savingCancelar.set(true);
    this.citaSvc.cancelar(id, { motivo: this.cancelarForm.value.motivo }).subscribe({
      next: () => { this.savingCancelar.set(false); this.closeCancelar(); this.loadMisCitas(); this.showSuccess('Cita cancelada'); },
      error: e  => { this.savingCancelar.set(false); this.error.set(e.error?.message ?? 'Error al cancelar'); },
    });
  }

  // ── Cliente: reprogramar ─────────────────────────────────

  openReprogramar(cita: CitaResponse) {
    this.citaSeleccionada.set(cita);
    this.reprogForm.reset({ empleadoPreferidoId: '' });
    this.slots.set([]);
    this.slotSelReprog.set(null);
    this.groomerDelSlotReprogId.set(null);
    this.showReprogModal.set(true);
  }

  closeReprog() { this.showReprogModal.set(false); this.citaSeleccionada.set(null); }

  onReprogGroomerChange() {
    this.slotSelReprog.set(null);
    this.groomerDelSlotReprogId.set(null);
    this.onReprogFechaChange();
  }

  onReprogFechaChange() {
    const v = this.reprogForm.value;
    const cita = this.citaSeleccionada();
    if (!v.fecha || !cita) return;
    this.loadingSlots.set(true);
    this.reprogForm.patchValue({ slot: '' });
    this.slotSelReprog.set(null);
    this.groomerDelSlotReprogId.set(null);
    const empleadoId = v.empleadoPreferidoId ? +v.empleadoPreferidoId : undefined;
    this.citaSvc.slots(v.fecha, cita.servicioId, cita.mascotaId, empleadoId, cita.id).subscribe({
      next: s => { this.slots.set(s); this.loadingSlots.set(false); },
      error: () => this.loadingSlots.set(false),
    });
  }

  submitReprog() {
    if (this.reprogForm.invalid || this.needsGroomerReprog() || this.savingReprog()) return;
    const v = this.reprogForm.value;
    const id = this.citaSeleccionada()!.id;
    this.savingReprog.set(true);
    this.citaSvc.reprogramar(id, {
      fechaHoraInicio: v.slot,
      notas: v.notas || undefined,
      empleadoPreferidoId: this.groomerDelSlotReprogId() ?? undefined,
    }).subscribe({
      next: () => { this.savingReprog.set(false); this.closeReprog(); this.loadMisCitas(); this.showSuccess('Cita reprogramada, queda en revisión'); },
      error: e  => { this.savingReprog.set(false); this.error.set(e.error?.message ?? 'Error al reprogramar'); },
    });
  }

  // ── Selección de slots (cliente) ─────────────────────────

  selectSlotSolicitar(s: SlotResponse) {
    this.slotSelSol.set(s);
    this.solicitarForm.patchValue({ slot: s.inicio });
    if (s.groomers.length === 1) {
      this.groomerDelSlotSolId.set(s.groomers[0].id);
      this.solicitarForm.patchValue({ empleadoPreferidoId: s.groomers[0].id });
    } else {
      this.groomerDelSlotSolId.set(null);
      this.solicitarForm.patchValue({ empleadoPreferidoId: null });
    }
  }

  selectGroomerDelSlotSol(id: number) {
    this.groomerDelSlotSolId.set(id);
    this.solicitarForm.patchValue({ empleadoPreferidoId: id });
  }

  selectSlotReprog(s: SlotResponse) {
    this.slotSelReprog.set(s);
    this.reprogForm.patchValue({ slot: s.inicio });
    if (s.groomers.length === 1) {
      this.groomerDelSlotReprogId.set(s.groomers[0].id);
      this.reprogForm.patchValue({ empleadoPreferidoId: s.groomers[0].id });
    } else {
      this.groomerDelSlotReprogId.set(null);
      this.reprogForm.patchValue({ empleadoPreferidoId: null });
    }
  }

  selectGroomerDelSlotReprog(id: number) {
    this.groomerDelSlotReprogId.set(id);
    this.reprogForm.patchValue({ empleadoPreferidoId: id });
  }

  needsGroomerSol(): boolean {
    return (this.slotSelSol()?.groomers?.length ?? 0) > 1 && !this.groomerDelSlotSolId();
  }

  needsGroomerReprog(): boolean {
    return (this.slotSelReprog()?.groomers?.length ?? 0) > 1 && !this.groomerDelSlotReprogId();
  }

  // ── Helpers ──────────────────────────────────────────────

  precioFinalConRecargo(c: CitaResponse): number {
    if (!c.recargoPorcentaje || c.recargoPorcentaje <= 0) return c.precioFinal;
    return +(c.precioFinal + c.precioFinal * c.recargoPorcentaje / 100).toFixed(2);
  }

  canCancelar(c: CitaResponse): boolean  { return ['EN_REVISION', 'ACEPTADO'].includes(c.estado); }
  canReprogramar(c: CitaResponse): boolean { return ['EN_REVISION', 'ACEPTADO'].includes(c.estado); }

  horasHastaCita(fechaInicio: string): number {
    return (new Date(fechaInicio).getTime() - Date.now()) / 3_600_000;
  }

  policyColor(cita: CitaResponse): 'yellow' | 'red' {
    if (cita.estado === 'EN_REVISION') return 'yellow';
    if (cita.estado === 'ACEPTADO') {
      return this.horasHastaCita(cita.fechaHoraInicio as unknown as string) < 24 ? 'red' : 'yellow';
    }
    return this.horasHastaCita(cita.fechaHoraInicio as unknown as string) < 24 ? 'red' : 'yellow';
  }

  policyMsg(cita: CitaResponse, accion: 'cancelar' | 'reprogramar'): string {
    if (cita.estado === 'EN_REVISION') {
      return accion === 'cancelar'
        ? 'Tu cita está en revisión. Una vez confirmada, podrás cancelarla sin cargo si lo haces con más de 24 horas de anticipación.'
        : 'Tu cita está en revisión. Una vez confirmada, podrás reprogramarla sin cargo si lo haces con más de 24 horas de anticipación.';
    }
    const horas = this.horasHastaCita(cita.fechaHoraInicio as unknown as string);
    if (horas < 24) {
      return accion === 'cancelar'
        ? 'Estás cancelando con menos de 24 horas de anticipación. Se aplicará un cargo adicional del 5% sobre tu siguiente servicio.'
        : 'Estás reprogramando con menos de 24 horas de anticipación. Se aplicará un cargo adicional del 5% sobre tu siguiente servicio.';
    }
    return accion === 'cancelar'
      ? `Tienes ${Math.floor(horas)}h para cancelar sin cargos. Cancelaciones con menos de 24h generan un recargo del 5%.`
      : `Tienes ${Math.floor(horas)}h para reprogramar sin cargos. Cambios con menos de 24h generan un recargo del 5%.`;
  }

  duracionBreakdown(): { label: string; valor: string }[] {
    const srv = this.servicioActual();
    const m   = this.mascotaActual();
    if (!srv || !m) return [];
    const items = [{ label: 'Duración base', valor: `${srv.duracionMinutos} min` }];
    const t  = (m.tamano ?? '').toUpperCase().trim();
    const tp = (m.temperamentoNombre ?? '').toUpperCase().trim();
    if (t === 'MEDIANO' || t === 'MEDIANA') items.push({ label: 'Tamaño mediano', valor: '+5 min' });
    else if (t === 'GRANDE')   items.push({ label: 'Tamaño grande',   valor: '+10 min' });
    else if (t === 'GIGANTE')  items.push({ label: 'Tamaño gigante',  valor: '+15 min' });
    if (tp === 'NERVIOSO' || tp === 'INQUIETO') items.push({ label: 'Temperamento',          valor: '+5 min' });
    else if (tp === 'AGRESIVO')                 items.push({ label: 'Temperamento agresivo', valor: '+10 min' });
    return items;
  }

  precioBreakdown(): { label: string; valor: string; highlight?: boolean }[] {
    const srv = this.servicioActual();
    const m   = this.mascotaActual();
    if (!srv || !m) return [];
    const items: { label: string; valor: string; highlight?: boolean }[] = [{ label: 'Precio base', valor: `Bs. ${srv.precioBase}` }];
    const t  = (m.tamano ?? '').toUpperCase().trim();
    const tp = (m.temperamentoNombre ?? '').toUpperCase().trim();
    if (t === 'MEDIANO' || t === 'MEDIANA') items.push({ label: 'Tamaño mediano', valor: '+Bs. 10' });
    else if (t === 'GRANDE')   items.push({ label: 'Tamaño grande',   valor: '+Bs. 15' });
    else if (t === 'GIGANTE')  items.push({ label: 'Tamaño gigante',  valor: '+Bs. 20' });
    if (tp === 'NERVIOSO' || tp === 'INQUIETO') items.push({ label: 'Temperamento',          valor: '+Bs. 5' });
    else if (tp === 'AGRESIVO')                 items.push({ label: 'Temperamento agresivo', valor: '+Bs. 10' });
    const pen = this.penalizacionActual();
    if (pen > 0) {
      const base = this.precioEstimado() ?? 0;
      const recargo = +(base * pen / 100).toFixed(2);
      items.push({ label: `Recargo por cancelación tardía (+${pen}%)`, valor: `+Bs. ${recargo}`, highlight: true });
    }
    return items;
  }

  temperamentoColor(nombre: string | null | undefined): string {
    const map: Record<string, string> = {
      'Tranquilo': '#4ade80',
      'Nervioso':  '#fbbf24',
      'Agresivo':  '#f87171',
      'Inquieto':  '#a78bfa',
    };
    return (nombre && map[nombre]) ? map[nombre] : '#94a3b8';
  }

  tamanoClass(t: string | null | undefined): string {
    if (!t) return '';
    const map: Record<string, string> = { PEQUEÑO: 'tam-p', MEDIANO: 'tam-m', GRANDE: 'tam-g', GIGANTE: 'tam-x' };
    return map[t.toUpperCase().trim()] ?? '';
  }

  estadoLabel(e: string): string {
    return ({ EN_REVISION: 'En revisión', ACEPTADO: 'Aceptado', PENDIENTE_PAGO: 'Por cobrar', REALIZADO: 'Realizado', CANCELADO: 'Cancelado' } as Record<string, string>)[e] ?? e;
  }

  estadoClass(e: string): string {
    return ({ EN_REVISION: 'revision', ACEPTADO: 'aceptado', PENDIENTE_PAGO: 'cobrar', REALIZADO: 'realizado', CANCELADO: 'cancelado' } as Record<string, string>)[e] ?? '';
  }

  formatDT(dt: string): string {
    if (!dt) return '—';
    const d = new Date(dt);
    return d.toLocaleDateString('es-BO', { day: '2-digit', month: 'short', year: 'numeric' })
      + ' ' + d.toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit' });
  }

  formatSlot(inicio: string, fin: string): string {
    const f = (s: string) => new Date(s).toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit' });
    return `${f(inicio)} – ${f(fin)}`;
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

  private showSuccess(msg: string) {
    this.success.set(msg);
    setTimeout(() => this.success.set(null), 3500);
  }
}
