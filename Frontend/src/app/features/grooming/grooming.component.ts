import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { CitaService, CitaResponse } from '../../core/services/cita.service';
import {
  GroomingService,
  FichaResponse,
  FotoResponse,
  InsumoResponse,
  InsumoRequest
} from '../../core/services/grooming.service';
import { InventarioService, ProductoResponse } from '../../core/services/inventario.service';
import { GroomingNotificacionService } from '../../core/services/grooming-notificacion.service';
import { StockNotificacionService } from '../../core/services/stock-notificacion.service';

@Component({
  selector: 'app-grooming',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './grooming.component.html',
  styleUrl: './grooming.component.scss',
})
export class GroomingComponent implements OnInit {
  private auth          = inject(AuthService);
  private svc           = inject(GroomingService);
  private citaSvc       = inject(CitaService);
  private invSvc        = inject(InventarioService);
  private groomingNotif = inject(GroomingNotificacionService);
  private stockNotif    = inject(StockNotificacionService);

  rol     = computed(() => this.auth.rol());
  canEdit = computed(() => this.rol() === 'GROOMER' || this.rol() === 'ADMIN');

  citas             = signal<CitaResponse[]>([]);
  citaSeleccionada  = signal<CitaResponse | null>(null);
  ficha             = signal<FichaResponse | null>(null);
  insumos           = signal<InsumoResponse[]>([]);
  productos         = signal<ProductoResponse[]>([]);

  estadoIngreso      = signal('NORMAL');
  observaciones      = signal('');
  detalles           = signal<string[]>([]);
  nuevoDetalle       = signal('');
  checklistUnas      = signal(false);
  checklistOidos     = signal(false);
  checklistGlandulas = signal(false);
  checklistCorte     = signal(false);
  checklistBano      = signal(false);
  checklistPerfume   = signal(false);
  checklistPeinado   = signal(false);
  recomendacion      = signal('');
  proximaCita        = signal('');

  activeTab     = signal<'ficha' | 'fotos' | 'insumos' | 'recomendacion'>('ficha');
  loading       = signal(false);
  uploadingFoto = signal(false);
  errorMsg      = signal('');
  successMsg    = signal('');

  minProximaCita = (() => {
    const d = new Date(); d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  })();

  showInsumoModal = signal(false);
  insumoItems     = signal<{ productoId: number | null; cantidad: number; notas: string; busqueda: string; open: boolean }[]>([]);
  insumoEnviando  = signal(false);

  checklistVisibles(servicio: string | null): Set<string> {
    if (!servicio) return new Set(['bano','corte','unas','oidos','glandulas','perfume','peinado']);
    const s = servicio.toLowerCase();
    if (s.includes('rápido') || s.includes('rapido')) return new Set(['bano','oidos']);
    if ((s.includes('baño') || s.includes('bano')) && !s.includes('servicio')) return new Set(['bano','oidos','unas']);
    if (s.includes('corte') || s.includes('peinado')) return new Set(['corte','peinado']);
    return new Set(['bano','corte','unas','oidos','glandulas','perfume','peinado']);
  }

  checklistTotal = computed(() =>
    this.checklistVisibles(this.citaSeleccionada()?.servicioNombre ?? null).size
  );

  checklistCount = computed(() => {
    const vis = this.checklistVisibles(this.citaSeleccionada()?.servicioNombre ?? null);
    let n = 0;
    if (vis.has('unas')     && this.checklistUnas())      n++;
    if (vis.has('oidos')    && this.checklistOidos())     n++;
    if (vis.has('glandulas') && this.checklistGlandulas()) n++;
    if (vis.has('corte')    && this.checklistCorte())     n++;
    if (vis.has('bano')     && this.checklistBano())      n++;
    if (vis.has('perfume')  && this.checklistPerfume())   n++;
    if (vis.has('peinado')  && this.checklistPeinado())   n++;
    return n;
  });
  fichaCerrada = computed(() => this.ficha()?.estado === 'CERRADA');

  esHoy = computed(() => {
    const cita = this.citaSeleccionada();
    if (!cita?.fechaHoraInicio) return false;
    const fecha = new Date(cita.fechaHoraInicio as any);
    if (isNaN(fecha.getTime())) return false;
    const hoy = new Date();
    return fecha.getFullYear() === hoy.getFullYear() &&
           fecha.getMonth()    === hoy.getMonth()    &&
           fecha.getDate()     === hoy.getDate();
  });
  tieneFotos        = computed(() => {
    const fotos = this.ficha()?.fotos ?? [];
    return ['ANTES','DURANTE','DESPUES'].every(m => fotos.some(f => f.momento === m));
  });
  insumosResueltos   = computed(() => this.insumos().every(i => i.estado !== 'SOLICITADO' && i.estado !== 'ENTREGADO'));
  tieneRecomendacion = computed(() => this.recomendacion().trim().length > 0 && this.proximaCita().trim().length > 0);
  fichaCompleta      = computed(() =>
    this.estadoIngreso().trim().length > 0 &&
    this.observaciones().trim().length > 0 &&
    this.detalles().length >= 1 &&
    this.checklistCount() === this.checklistTotal()
  );
  puedesCerrar       = computed(() =>
    this.fichaCompleta() &&
    this.tieneFotos() &&
    this.insumosResueltos() &&
    this.tieneRecomendacion()
  );

  estadosIngreso = ['NORMAL', 'NUDOS', 'HERIDAS', 'PULGAS', 'SUCIEDAD_EXTREMA', 'AGRESIVO'];

  ngOnInit() {
    this.citaSvc.misServicios().subscribe({
      next: data => this.citas.set(data.filter(c => c.estado === 'ACEPTADO')),
      error: () => this.setError('Error al cargar servicios activos')
    });
    this.invSvc.getProductos().subscribe({ next: p => this.productos.set(p) });
  }

  seleccionarCita(cita: CitaResponse) {
    this.citaSeleccionada.set(cita);
    this.ficha.set(null);
    this.insumos.set([]);
    this.errorMsg.set('');
    this.successMsg.set('');
    this.activeTab.set('ficha');

    this.svc.getFichaPorCita(cita.id).subscribe({
      next: f => { this.ficha.set(f); if (f) this.poblarForm(f); else this.resetForm(); },
      error: () => this.resetForm()
    });
    this.svc.getInsumosPorCita(cita.id).subscribe({
      next: ins => this.insumos.set(ins),
      error: () => {}
    });
  }

  private poblarForm(f: FichaResponse) {
    this.estadoIngreso.set(f.estadoIngreso || 'NORMAL');
    this.observaciones.set(f.observacionesGenerales || '');
    this.detalles.set(f.detalles ? [...f.detalles] : []);
    this.checklistUnas.set(f.checklistUnas || false);
    this.checklistOidos.set(f.checklistOidos || false);
    this.checklistGlandulas.set(f.checklistGlandulas || false);
    this.checklistCorte.set(f.checklistCorte || false);
    this.checklistBano.set(f.checklistBano || false);
    this.checklistPerfume.set(f.checklistPerfume || false);
    this.checklistPeinado.set(f.checklistPeinado || false);
    this.recomendacion.set(f.recomendacion || '');
    this.proximaCita.set(f.proximaCitaSugerida || '');
  }

  private resetForm() {
    this.estadoIngreso.set('NORMAL');
    this.observaciones.set('');
    this.detalles.set([]);
    this.checklistUnas.set(false);
    this.checklistOidos.set(false);
    this.checklistGlandulas.set(false);
    this.checklistCorte.set(false);
    this.checklistBano.set(false);
    this.checklistPerfume.set(false);
    this.checklistPeinado.set(false);
    this.recomendacion.set('');
    this.proximaCita.set('');
  }

  cerrarFicha() {
    const f    = this.ficha();
    const cita = this.citaSeleccionada();
    const ins  = this.insumos();

    if ((f?.fotos ?? []).length === 0) {
      this.activeTab.set('fotos');
      this.setError('Debes subir al menos una foto del servicio');
      return;
    }
    if (ins.some(i => i.estado === 'SOLICITADO' || i.estado === 'ENTREGADO')) {
      this.activeTab.set('insumos');
      this.setError('Hay insumos pendientes. Márcalos como Usado, Devuelto o Desperdiciado antes de cerrar');
      return;
    }
    if (!this.recomendacion().trim()) {
      this.activeTab.set('recomendacion');
      this.setError('La recomendación para el cliente es obligatoria');
      return;
    }
    if (!f || !cita) return;

    this.loading.set(true);
    this.errorMsg.set('');

    const req = {
      citaId:                 cita.id,
      estadoIngreso:          this.estadoIngreso(),
      observacionesGenerales: this.observaciones(),
      detalles:               this.detalles(),
      checklistUnas:          this.checklistUnas(),
      checklistOidos:         this.checklistOidos(),
      checklistGlandulas:     this.checklistGlandulas(),
      checklistCorte:         this.checklistCorte(),
      checklistBano:          this.checklistBano(),
      checklistPerfume:       this.checklistPerfume(),
      checklistPeinado:       this.checklistPeinado(),
      recomendacion:          this.recomendacion(),
      proximaCitaSugerida:    this.proximaCita() || undefined
    };

    this.svc.guardarFicha(req).subscribe({
      next: saved => {
        this.svc.cerrarFicha(saved.id).subscribe({
          next: updated => {
            this.ficha.set(updated);
            this.groomingNotif.refresh();
            this.stockNotif.refresh();
            this.setSuccess('Servicio cerrado — pendiente de cobro');
            this.loading.set(false);
            this.citaSvc.misServicios().subscribe({
              next: data => {
                this.citas.set(data.filter(c => c.estado === 'ACEPTADO'));
                this.citaSeleccionada.set(null);
                this.ficha.set(null);
                this.insumos.set([]);
                this.resetForm();
              },
              error: () => {}
            });
          },
          error: e => { this.setError(e.error?.message || 'Error al cerrar'); this.loading.set(false); }
        });
      },
      error: e => { this.setError(e.error?.message || 'Error al guardar la ficha'); this.loading.set(false); }
    });
  }

  agregarDetalle() {
    const d = this.nuevoDetalle().trim();
    if (!d) return;
    this.detalles.update(arr => [...arr, d]);
    this.nuevoDetalle.set('');
  }

  eliminarDetalle(i: number) {
    this.detalles.update(arr => arr.filter((_, idx) => idx !== i));
  }

  onFotoChange(event: Event, momento: string) {
    const file = (event.target as HTMLInputElement).files?.[0];
    const cita = this.citaSeleccionada();
    if (!file || !cita) return;
    this.uploadingFoto.set(true);
    this.svc.subirFoto(cita.id, file, momento).subscribe({
      next: foto => {
        this.ficha.update(f => f ? { ...f, fotos: [...(f.fotos ?? []), foto] } : f);
        this.uploadingFoto.set(false);
      },
      error: () => { this.setError('Error al subir foto'); this.uploadingFoto.set(false); }
    });
    (event.target as HTMLInputElement).value = '';
  }

  eliminarFoto(fotoId: number) {
    this.svc.eliminarFoto(fotoId).subscribe({
      next: () => this.ficha.update(f => f ? { ...f, fotos: f.fotos.filter(fo => fo.id !== fotoId) } : f)
    });
  }

  abrirModalInsumo() {
    this.insumoItems.set([{ productoId: null, cantidad: 1, notas: '', busqueda: '', open: false }]);
    this.showInsumoModal.set(true);
  }

  agregarItemInsumo() {
    this.insumoItems.update(arr => [...arr, { productoId: null, cantidad: 1, notas: '', busqueda: '', open: false }]);
  }

  quitarItemInsumo(i: number) {
    this.insumoItems.update(arr => arr.filter((_, idx) => idx !== i));
  }

  setItemProducto(i: number, pid: number | null) {
    const nombre = pid ? (this.productos().find(p => p.id === pid)?.nombre ?? '') : '';
    this.insumoItems.update(arr => arr.map((item, idx) => idx === i ? { ...item, productoId: pid, busqueda: nombre, open: false } : item));
  }

  setItemBusqueda(i: number, val: string) {
    this.insumoItems.update(arr => arr.map((item, idx) => idx === i ? { ...item, busqueda: val, productoId: null, open: true } : item));
  }

  openItemDropdown(i: number) {
    this.insumoItems.update(arr => arr.map((item, idx) => idx === i ? { ...item, open: true } : item));
  }

  closeItemDropdown(i: number) {
    this.insumoItems.update(arr => arr.map((item, idx) => idx === i ? { ...item, open: false } : item));
  }

  selectProductoItem(i: number, p: ProductoResponse) {
    this.insumoItems.update(arr => arr.map((item, idx) => idx === i ? { ...item, productoId: p.id, busqueda: p.nombre, open: false } : item));
  }

  filtrarProductos(busqueda: string): ProductoResponse[] {
    const q = busqueda.toLowerCase().trim();
    if (!q) return this.productos();
    return this.productos().filter(p => p.nombre.toLowerCase().includes(q));
  }

  setItemCantidad(i: number, cant: number) {
    this.insumoItems.update(arr => arr.map((item, idx) => idx === i ? { ...item, cantidad: cant } : item));
  }

  setItemNotas(i: number, notas: string) {
    this.insumoItems.update(arr => arr.map((item, idx) => idx === i ? { ...item, notas } : item));
  }

  stockDeProducto(pid: number | null): number | null {
    if (!pid) return null;
    return this.productos().find(p => p.id === pid)?.stockActual ?? null;
  }

  insumoItemsValidos = computed(() =>
    this.insumoItems().length > 0 && this.insumoItems().every(i => i.productoId && i.cantidad >= 1)
  );

  registrarInsumo() {
    const cita  = this.citaSeleccionada();
    const items = this.insumoItems().filter(i => i.productoId && i.cantidad >= 1);
    if (!cita || items.length === 0) return;
    this.insumoEnviando.set(true);
    let completados = 0;
    let errores = 0;
    for (const item of items) {
      const req: InsumoRequest = {
        citaId: cita.id,
        empleadoId: 0,
        productoId: item.productoId!,
        cantidad: item.cantidad,
        notas: item.notas || undefined
      };
      this.svc.solicitarInsumo(req).subscribe({
        next: ins => {
          this.insumos.update(arr => [ins, ...arr]);
          this.productos.update(ps => ps.map(p =>
            p.id === ins.productoId ? { ...p, stockActual: p.stockActual - ins.cantidad } : p
          ));
          completados++;
          if (completados + errores === items.length) {
            this.insumoEnviando.set(false);
            this.showInsumoModal.set(false);
            this.setSuccess(`${completados} solicitud(es) enviada(s) — esperando aprobación de recepción`);
          }
        },
        error: e => {
          errores++;
          if (completados + errores === items.length) {
            this.insumoEnviando.set(false);
            this.showInsumoModal.set(false);
            if (completados > 0) this.setSuccess(`${completados} enviada(s), ${errores} con error`);
            else this.setError(e.error?.message || 'Error al enviar solicitud');
          }
        }
      });
    }
  }

  actualizarEstadoInsumo(insumo: InsumoResponse, estado: string) {
    this.svc.actualizarEstadoInsumo(insumo.id, estado).subscribe({
      next: updated => {
        this.insumos.update(arr => arr.map(i => i.id === updated.id ? updated : i));
        const devuelveStock =
          (estado === 'RECHAZADO' && insumo.estado === 'SOLICITADO') ||
          (estado === 'DEVUELTO'  && insumo.estado === 'ENTREGADO');
        if (devuelveStock) {
          this.productos.update(ps => ps.map(p =>
            p.id === updated.productoId ? { ...p, stockActual: p.stockActual + updated.cantidad } : p
          ));
        }
      }
    });
  }

  fotosPorMomento(momento: string): FotoResponse[] {
    return this.ficha()?.fotos?.filter(f => f.momento === momento) ?? [];
  }

  private setError(msg: string) {
    this.errorMsg.set(msg); this.successMsg.set('');
    setTimeout(() => this.errorMsg.set(''), 5000);
  }

  private setSuccess(msg: string) {
    this.successMsg.set(msg); this.errorMsg.set('');
    setTimeout(() => this.successMsg.set(''), 4000);
  }

  formatEstado(e: string): string {
    const map: Record<string, string> = {
      NORMAL: 'Normal', NUDOS: 'Nudos', HERIDAS: 'Heridas',
      PULGAS: 'Pulgas', SUCIEDAD_EXTREMA: 'Suciedad extrema', AGRESIVO: 'Agresivo'
    };
    return map[e] ?? e;
  }

  estadoInsumoLabel(e: string): string {
    const map: Record<string, string> = {
      SOLICITADO: 'Solicitado', ENTREGADO: 'Entregado', USADO: 'Usado',
      DEVUELTO: 'Devuelto', DESPERDICIADO: 'Desperdiciado', RECHAZADO: 'Rechazado'
    };
    return map[e] ?? e;
  }

  estadoLabel(e: string): string {
    return ({ EN_REVISION: 'En revisión', ACEPTADO: 'Aceptado', PENDIENTE_PAGO: 'Por cobrar', REALIZADO: 'Realizado', CANCELADO: 'Cancelado' } as Record<string, string>)[e] ?? e;
  }

  estadoClass(e: string): string {
    return ({ EN_REVISION: 'revision', ACEPTADO: 'aceptado', PENDIENTE_PAGO: 'cobrar', REALIZADO: 'realizado', CANCELADO: 'cancelado' } as Record<string, string>)[e] ?? '';
  }

  tamanoClass(t: string | null | undefined): string {
    if (!t) return '';
    const map: Record<string, string> = { PEQUEÑO: 'tam-p', MEDIANO: 'tam-m', GRANDE: 'tam-g', GIGANTE: 'tam-x' };
    return map[t.toUpperCase().trim()] ?? '';
  }

  temperamentoColor(nombre: string | null | undefined): string {
    const map: Record<string, string> = {
      'Tranquilo': '#4ade80', 'Nervioso': '#fbbf24', 'Agresivo': '#f87171', 'Inquieto': '#a78bfa',
    };
    return (nombre && map[nombre]) ? map[nombre] : '#94a3b8';
  }

  formatDT(dt: string | null | undefined): string {
    if (!dt) return '—';
    const d = new Date(dt);
    return d.toLocaleDateString('es-BO', { day: '2-digit', month: 'short', year: 'numeric' })
      + ' ' + d.toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit' });
  }
}
