import { jsPDF } from 'jspdf';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { PerfilResponse } from '../../models/auth.model';
import { ConfirmService } from '../../core/services/confirm.service';
import {
  InventarioService,
  CategoriaResponse,
  ProductoResponse,
  PromocionResponse,
  CuponResponse,
  PedidoResponse,
  MetodoPagoResponse,
  VentaResponse,
  CartItem,
  CuponValidadoResponse,
} from '../../core/services/inventario.service';
import { environment } from '../../../environments/environment';
import { SearchBarComponent } from '../../shared/components/search-bar/search-bar.component';

type AdminTab = 'productos' | 'categorias' | 'promociones' | 'cupones';

const FILE_BASE = environment.apiUrl.replace(/\/api$/, '');
type StaffTab  = 'tienda' | 'pedidos';
type ClienteTab = 'tienda' | 'carrito' | 'pedidos';

interface ClienteItem { id: number; nombre: string; ci?: string; telefono?: string; correo?: string; canalPreferido?: string; }

const TIPOS_PROMO = ['TEMPORADA', 'CAMPANA', 'CLIENTE_FRECUENTE', 'OTRO'];

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [ReactiveFormsModule, NgClass, SearchBarComponent],
  templateUrl: './inventario.component.html',
  styleUrl: './inventario.component.scss',
})
export class InventarioComponent implements OnInit, OnDestroy {
  private svc     = inject(InventarioService);
  private auth    = inject(AuthService);
  private confirm = inject(ConfirmService);
  private fb      = inject(FormBuilder);
  private http    = inject(HttpClient);

  private refreshInterval: ReturnType<typeof setInterval> | null = null;

  isAdmin     = computed(() => this.auth.rol() === 'ADMIN');
  isRecepcion = computed(() => this.auth.rol() === 'RECEPCION');
  isGroomer   = computed(() => this.auth.rol() === 'GROOMER');
  isCliente   = computed(() => this.auth.rol() === 'CLIENTE');
  isStaff     = computed(() => ['ADMIN','RECEPCION'].includes(this.auth.rol() ?? ''));

  categorias    = signal<CategoriaResponse[]>([]);
  productos     = signal<ProductoResponse[]>([]);
  metodosPago   = signal<MetodoPagoResponse[]>([]);
  loading       = signal(false);
  error         = signal<string | null>(null);
  success       = signal<string | null>(null);
  saving        = signal(false);

  adminTab      = signal<AdminTab>('productos');
  promociones   = signal<PromocionResponse[]>([]);
  cupones       = signal<CuponResponse[]>([]);

  showProductoModal  = signal(false);
  editingProducto    = signal<ProductoResponse | null>(null);
  selectedImage      = signal<File | null>(null);
  selectedImageName  = signal('');

  showCategoriaModal = signal(false);
  editingCategoria   = signal<CategoriaResponse | null>(null);

  showPromoModal     = signal(false);
  editingPromo       = signal<PromocionResponse | null>(null);

  showCuponModal     = signal(false);
  editingCupon       = signal<CuponResponse | null>(null);

  staffTab         = signal<StaffTab>('tienda');
  clientes         = signal<ClienteItem[]>([]);
  selectedCliente  = signal<ClienteItem | null>(null);
  pedidosAdmin     = signal<PedidoResponse[]>([]);
  showClienteModal = signal(false);
  clienteSearch       = signal('');
  promoProductoSearch = signal('');
  cuponProductoSearch = signal('');

  clienteTab    = signal<ClienteTab>('tienda');
  pedidos       = signal<VentaResponse[]>([]);

  cart          = signal<CartItem[]>([]);
  filtroCategoria = signal<number | null>(null);
  searchQuery   = signal('');
  cuponCode     = signal('');
  cuponValidado = signal<CuponValidadoResponse | null>(null);
  validandoCupon = signal(false);
  descuentoManual = signal<number>(0);
  telefonoCheckout = signal('');
  tipoEntrega   = signal<string>('RECOGER');
  direccionEntrega = signal<string>('');
  selectedMetodoPago = signal<number | null>(null);
  showCheckoutModal = signal(false);
  ventaCreada    = signal<VentaResponse | null>(null);

  promoProductoIds = signal<number[]>([]);
  cuponProductoIds = signal<number[]>([]);

  miPerfil = signal<PerfilResponse | null>(null);


  productoForm!:  FormGroup;
  categoriaForm!: FormGroup;
  promoForm!:     FormGroup;
  cuponForm!:     FormGroup;

  tiposPromo = TIPOS_PROMO;
  today      = new Date().toISOString().split('T')[0];

  productosFiltrados = computed(() => {
    let lista = this.productos();
    const cat = this.filtroCategoria();
    const q   = this.searchQuery().toLowerCase().trim();
    if (cat) lista = lista.filter(p => p.categoriaId === cat);
    if (q)   lista = lista.filter(p => p.nombre.toLowerCase().includes(q) || (p.descripcion ?? '').toLowerCase().includes(q));
    return lista;
  });

  cartSubtotal = computed(() =>
    this.cart().reduce((acc, i) => acc + this.efectivoPrecio(i.producto) * i.cantidad, 0)
  );

  efectivoPrecio(p: ProductoResponse): number {
    return p.descuentoPct > 0 ? p.precioFinal : p.precioVenta;
  }

  cartDescuento = computed(() =>
    (this.cuponValidado()?.montoDescuento ?? 0) + this.descuentoManual()
  );

  cartTotal = computed(() =>
    Math.max(0, this.cartSubtotal() - this.cartDescuento())
  );

  cartCount = computed(() =>
    this.cart().reduce((acc, i) => acc + i.cantidad, 0)
  );

  frecuentePct = computed(() => {
    const count = this.isCliente()
      ? this.pedidos().filter(v => v.estadoPedido === 'ENTREGADO').length
      : 0;
    return Math.min(5, Math.floor(count / 10));
  });

  descuentoFrecuenteEstimado = computed(() => {
    const pct = this.frecuentePct();
    return pct > 0 ? Math.round(this.cartSubtotal() * pct) / 100 : 0;
  });

  clientesFiltrados = computed(() => {
    const q = this.clienteSearch().toLowerCase().trim();
    if (!q) return this.clientes();
    return this.clientes().filter(c =>
      c.nombre.toLowerCase().includes(q) || (c.ci ?? '').toLowerCase().includes(q)
    );
  });

  productosParaPromo = computed(() => {
    const q = this.promoProductoSearch().toLowerCase().trim();
    if (!q) return this.productos();
    return this.productos().filter(p => p.nombre.toLowerCase().includes(q));
  });

  productosParaCupon = computed(() => {
    const q = this.cuponProductoSearch().toLowerCase().trim();
    if (!q) return this.productos();
    return this.productos().filter(p => p.nombre.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    this.buildForms();
    this.svc.getCategorias().subscribe({ next: c => this.categorias.set(c) });
    this.svc.getMetodosPago().subscribe({ next: m => this.metodosPago.set(m) });

    if (this.isAdmin()) {
      this.loadProductosAdmin();
      this.loadPromociones();
      this.loadCupones();
      this.refreshInterval = setInterval(() => this.silentRefreshProductos(), 60_000);
    } else if (this.isRecepcion()) {
      this.loadCatalogo();
      this.loadClientes();
      this.loadPedidosAdmin();
      this.refreshInterval = setInterval(() => this.silentRefreshCatalogo(), 60_000);
    } else {
      this.loadCatalogo();
      if (this.isCliente()) {
        this.loadPedidos();
        this.auth.getPerfil().subscribe({ next: p => this.miPerfil.set(p) });
      }
    }
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  private silentRefreshProductos(): void {
    this.svc.getProductosAdmin().subscribe({ next: p => this.productos.set(p) });
  }

  private silentRefreshCatalogo(): void {
    this.svc.getProductos().subscribe({ next: p => this.productos.set(p) });
  }

  loadCatalogo() {
    this.loading.set(true);
    this.svc.getProductos().subscribe({
      next: p  => { this.productos.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  loadProductosAdmin() {
    this.loading.set(true);
    this.svc.getProductosAdmin().subscribe({
      next: p  => { this.productos.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  loadPromociones() {
    this.svc.getPromocionesAdmin().subscribe({ next: p => this.promociones.set(p) });
  }

  loadCupones() {
    this.svc.getCuponesAdmin().subscribe({ next: c => this.cupones.set(c) });
  }

  loadPedidos() {
    this.svc.misPedidos().subscribe({
      next: p  => this.pedidos.set(p),
      error: e => {
        console.error('[mis-pedidos]', e.status, e.error);
        this.pedidos.set([]);
      },
    });
  }

  loadPedidosAdmin() {
    this.svc.listarPedidosAdmin().subscribe({
      next: p  => this.pedidosAdmin.set(p),
      error: e => console.error('[pedidos-admin]', e.status, e.error),
    });
  }

  loadClientes() {
    this.http.get<any[]>(`${environment.apiUrl}/clientes`).subscribe({
      next: data => this.clientes.set(data.map(c => ({
        id: c.id, nombre: c.nombre ?? c.correo,
        ci: c.ci ?? undefined,
        telefono: c.telefono, correo: c.correo,
        canalPreferido: c.preferencias?.['canal_preferido'],
      }))),
    });
  }

  seleccionarCliente(c: ClienteItem) {
    this.selectedCliente.set(c);
    this.showClienteModal.set(false);
    this.clienteSearch.set('');
  }

  addToCart(p: ProductoResponse) {
    const existing = this.cart().find(i => i.producto.id === p.id);
    if (existing) {
      if (existing.cantidad >= p.stockActual) return;
      this.cart.update(c => c.map(i => i.producto.id === p.id ? { ...i, cantidad: i.cantidad + 1 } : i));
    } else {
      this.cart.update(c => [...c, { producto: p, cantidad: 1 }]);
    }
  }

  removeFromCart(id: number) {
    this.cart.update(c => c.filter(i => i.producto.id !== id));
  }

  setQty(id: number, qty: number) {
    if (qty <= 0) { this.removeFromCart(id); return; }
    this.cart.update(c => c.map(i => i.producto.id === id ? { ...i, cantidad: Math.min(qty, i.producto.stockActual) } : i));
  }

  clearCart() {
    this.cart.set([]);
    this.cuponValidado.set(null);
    this.cuponCode.set('');
    this.descuentoManual.set(0);
    this.selectedCliente.set(null);
    this.telefonoCheckout.set('');
    this.tipoEntrega.set('RECOGER');
    this.direccionEntrega.set('');
  }

  aplicarCupon() {
    const code = this.cuponCode().trim();
    if (!code) return;
    this.validandoCupon.set(true);
    const ids = this.cart().map(i => i.producto.id);
    this.svc.validarCupon(code, ids, this.cartSubtotal()).subscribe({
      next: cv => { this.cuponValidado.set(cv); this.validandoCupon.set(false); this.showSuccess('Cupón aplicado'); },
      error: e  => { this.error.set(this.apiErr(e) ?? 'Cupón inválido'); this.validandoCupon.set(false); },
    });
  }

  quitarCupon() {
    this.cuponValidado.set(null);
    this.cuponCode.set('');
  }

  openCheckout() {
    if (this.cart().length === 0) return;
    if (this.isRecepcion() && !this.selectedCliente()) {
      this.error.set('Selecciona un cliente antes de continuar'); return;
    }
    if (!this.selectedMetodoPago()) {
      this.metodosPago().length && this.selectedMetodoPago.set(this.metodosPago()[0].id);
    }
    if (this.isCliente()) {
      const p = this.miPerfil();
      if (p?.telefono && !this.telefonoCheckout()) this.telefonoCheckout.set(p.telefono);
      if (p?.direccion && !this.direccionEntrega())  this.direccionEntrega.set(p.direccion);
    }
    this.showCheckoutModal.set(true);
  }

  confirmarVenta() {
    if (this.saving()) return;
    const clienteActual = this.isCliente()
      ? null
      : this.selectedCliente()?.id ?? null;

    const clienteTelf = this.isCliente()
      ? (this.miPerfil()?.telefono || this.telefonoCheckout())
      : (this.selectedCliente()?.telefono || this.telefonoCheckout());

    const direccionFinal = this.tipoEntrega() === 'DOMICILIO'
      ? (this.direccionEntrega() || this.miPerfil()?.direccion || undefined)
      : undefined;

    const req = {
      clienteId:        clienteActual ?? undefined,
      metodoPagoId:     this.selectedMetodoPago()!,
      codigoCupon:      this.cuponValidado() ? this.cuponCode() : undefined,
      descuentoManual:  this.isRecepcion() ? this.descuentoManual() || undefined : undefined,
      items:            this.cart().map(i => ({ productoId: i.producto.id, cantidad: i.cantidad })),
      telefonoContacto: clienteTelf || undefined,
      tipoEntrega:      this.isRecepcion() ? 'RECOGER' : this.tipoEntrega(),
      direccionEntrega: direccionFinal,
    };

    this.saving.set(true);
    this.svc.crearVenta(req).subscribe({
      next: venta => {
        this.ventaCreada.set(venta);
        this.saving.set(false);
        this.showCheckoutModal.set(false);
        this.clearCart();
        if (this.isCliente()) {
          this.loadPedidos();
          this.loadCatalogo();
          this.actualizarPerfilSiCambio(clienteTelf, req.direccionEntrega);
        }
        if (this.isRecepcion()) { this.loadPedidosAdmin(); this.loadCatalogo(); }
        if (this.isAdmin()) { this.loadPedidosAdmin(); this.loadProductosAdmin(); }
      },
      error: e => { this.error.set(this.apiErr(e) ?? 'Error al procesar la venta'); this.saving.set(false); },
    });
  }

  private actualizarPerfilSiCambio(nuevoTelf?: string, nuevaDir?: string): void {
    const p = this.miPerfil();
    const cambios: Record<string, string> = {};
    if (nuevoTelf && nuevoTelf !== p?.telefono) cambios['telefono'] = nuevoTelf;
    if (nuevaDir  && nuevaDir  !== p?.direccion) cambios['direccion'] = nuevaDir;
    if (Object.keys(cambios).length === 0) return;
    this.auth.updatePerfil(cambios).subscribe({
      next: perfil => this.miPerfil.set(perfil),
    });
  }

  cerrarVentaCreada() { this.ventaCreada.set(null); }

  async entregarPedido(p: PedidoResponse) {
    const ok = await this.confirm.confirm({
      title: 'Marcar como entregado',
      message: `¿Confirmar entrega del pedido #${p.ventaId} a ${p.clienteNombre ?? '—'}?`,
      confirmLabel: 'Entregar',
    });
    if (!ok) return;
    this.svc.entregarPedido(p.id).subscribe({
      next: () => {
        this.loadPedidosAdmin();
        this.showSuccess('Pedido entregado correctamente');
        if (p.clienteTelefono) {
          this.svc.abrirWhatsApp(p.clienteTelefono, this.generarMensajePedido(p));
        }
      },
      error: e => this.error.set(this.apiErr(e) ?? 'Error al entregar pedido'),
    });
  }

  async cancelarPedidoStaff(p: PedidoResponse) {
    const ok = await this.confirm.confirm({
      title: 'Cancelar pedido',
      message: `¿Cancelar el pedido #${p.ventaId}? Se restaurará el stock.`,
      confirmLabel: 'Cancelar pedido', danger: true,
    });
    if (!ok) return;
    this.svc.cancelarPedido(p.id).subscribe({
      next: () => { this.loadPedidosAdmin(); this.showSuccess('Pedido cancelado'); },
      error: e  => this.error.set(this.apiErr(e) ?? 'Error al cancelar pedido'),
    });
  }

  async cancelarMiPedido(v: VentaResponse) {
    const ok = await this.confirm.confirm({
      title: 'Cancelar pedido',
      message: `¿Cancelar el pedido #${v.id}? Se restaurará el stock.`,
      confirmLabel: 'Sí, cancelar', danger: true,
    });
    if (!ok) return;
    this.svc.cancelarPedido(v.pedidoId!).subscribe({
      next: () => { this.loadPedidos(); this.showSuccess('Pedido cancelado'); },
      error: e  => this.error.set(this.apiErr(e) ?? 'Error al cancelar'),
    });
  }

  private buildReciboHtml(venta: VentaResponse): string {
    const fecha = new Date(venta.fechaVenta as any).toLocaleString('es-BO', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
    const filas = venta.items.map(i =>
      `<tr><td>${i.productoNombre}</td><td style="text-align:center">${i.cantidad}</td><td style="text-align:right">Bs. ${i.precioUnitario.toFixed(2)}</td><td style="text-align:right">Bs. ${i.subtotalItem.toFixed(2)}</td></tr>`
    ).join('');
    const descuentos = [
      venta.descuentoCupon > 0 ? `<tr><td colspan="3">Cupón (${venta.codigoCupon})</td><td style="text-align:right">-Bs. ${venta.descuentoCupon.toFixed(2)}</td></tr>` : '',
      venta.descuentoFrecuente > 0 ? `<tr><td colspan="3">Desc. frecuente</td><td style="text-align:right">-Bs. ${venta.descuentoFrecuente.toFixed(2)}</td></tr>` : '',
      venta.descuentoManual > 0 ? `<tr><td colspan="3">Desc. especial</td><td style="text-align:right">-Bs. ${venta.descuentoManual.toFixed(2)}</td></tr>` : '',
    ].join('');
    return `<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
<title>Recibo #${venta.id}</title>
<style>body{font-family:'Courier New',monospace;width:300px;margin:0 auto;padding:12px;font-size:12px}
h1{text-align:center;font-size:15px;margin:4px 0}
.sub{text-align:center;color:#555;font-size:10px;margin:2px 0}
hr{border:0;border-top:1px dashed #999;margin:8px 0}
table{width:100%;border-collapse:collapse}
td,th{padding:2px 3px}
th{border-bottom:1px solid #ccc;font-size:11px}
.tot td{font-weight:bold;border-top:1px solid #ccc;font-size:13px}
.center{text-align:center}
@media print{button{display:none}}</style></head>
<body>
<h1>🐾 SpaMascotas</h1>
<p class="sub">Recibo de Compra</p>
<hr>
<p><b>Pedido #${venta.id}</b><br>
${fecha}<br>
Cliente: ${venta.clienteNombre ?? '—'}<br>
${venta.clienteTelefono ? 'Tel: ' + venta.clienteTelefono + '<br>' : ''}
Pago: ${venta.metodoPago ?? '—'}</p>
<hr>
<table><thead><tr><th>Producto</th><th>Qty</th><th>P/U</th><th>Total</th></tr></thead>
<tbody>${filas}</tbody></table>
<hr>
<table>
<tr><td colspan="3">Subtotal</td><td style="text-align:right">Bs. ${venta.subtotal.toFixed(2)}</td></tr>
${descuentos}
<tr class="tot"><td colspan="3">TOTAL</td><td style="text-align:right">Bs. ${venta.totalFinal.toFixed(2)}</td></tr>
</table>
<hr>
<p class="center">¡Gracias por su compra! 🐾</p>
<button onclick="window.print()" style="width:100%;margin-top:8px;padding:6px;cursor:pointer;font-size:12px">🖨 Imprimir / Guardar PDF</button>
</body></html>`;
  }

  private buildReciboPedidoHtml(p: PedidoResponse): string {
    const fecha = new Date(p.fechaVenta as any).toLocaleString('es-BO', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
    return `<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
<title>Recibo Pedido #${p.ventaId}</title>
<style>body{font-family:'Courier New',monospace;width:300px;margin:0 auto;padding:12px;font-size:12px}
h1{text-align:center;font-size:15px;margin:4px 0}
.sub{text-align:center;color:#555;font-size:10px;margin:2px 0}
hr{border:0;border-top:1px dashed #999;margin:8px 0}
.center{text-align:center}
.total{font-weight:bold;font-size:14px;text-align:center;margin-top:8px}
@media print{button{display:none}}</style></head>
<body>
<h1>🐾 SpaMascotas</h1>
<p class="sub">Recibo de Compra — ENTREGADO</p>
<hr>
<p><b>Pedido #${p.ventaId}</b><br>
${fecha}<br>
Cliente: ${p.clienteNombre ?? '—'}<br>
${p.clienteTelefono ? 'Tel: ' + p.clienteTelefono + '<br>' : ''}</p>
<hr>
<p>${p.itemsResumen}</p>
<hr>
<p class="total">TOTAL: Bs. ${p.totalFinal.toFixed(2)}</p>
<hr>
<p class="center">¡Gracias por su compra! 🐾</p>
<button onclick="window.print()" style="width:100%;margin-top:8px;padding:6px;cursor:pointer;font-size:12px">🖨 Imprimir / Guardar PDF</button>
</body></html>`;
  }

  imprimirRecibo(venta: VentaResponse): void {
    const html = this.buildReciboHtml(venta);
    const w = window.open('', 'recibo', 'width=370,height=620,scrollbars=yes');
    if (!w) { this.error.set('Permite ventanas emergentes para generar el PDF'); return; }
    w.document.write(html); w.document.close(); w.focus();
    setTimeout(() => w.print(), 500);
  }

  descargarRecibo(venta: VentaResponse): void {
    const doc = new jsPDF({ unit: 'mm', format: 'a5', orientation: 'portrait' });
    const W = 148, L = 10, R = 138;
    let y = 16;
    const hr = () => { doc.setDrawColor(160); doc.line(L, y, R, y); y += 5; };

    doc.setFont('helvetica', 'bold'); doc.setFontSize(15);
    doc.text('SpaMascotas', W / 2, y, { align: 'center' }); y += 6;
    doc.setFont('helvetica', 'normal'); doc.setFontSize(9);
    doc.text('Recibo de Compra', W / 2, y, { align: 'center' }); y += 6;
    hr();

    const fecha = new Date(venta.fechaVenta as any).toLocaleString('es-BO', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold'); doc.text(`Pedido #${venta.id}`, L, y); y += 4;
    doc.setFont('helvetica', 'normal');
    doc.text(fecha, L, y); y += 4;
    doc.text(`Cliente: ${venta.clienteNombre ?? '-'}`, L, y); y += 4;
    if (venta.clienteCi) { doc.text(`CI: ${venta.clienteCi}`, L, y); y += 4; }
    if (venta.clienteTelefono) { doc.text(`Tel: ${venta.clienteTelefono}`, L, y); y += 4; }
    doc.text(`Pago: ${venta.metodoPago ?? '-'}`, L, y); y += 5;
    hr();

    doc.setFontSize(8); doc.setFont('helvetica', 'bold');
    const c1 = L, c2 = 80, c3 = 108, c4 = R;
    doc.text('Producto', c1, y);
    doc.text('Cant.', c2, y, { align: 'right' });
    doc.text('Precio', c3, y, { align: 'right' });
    doc.text('Subtotal', c4, y, { align: 'right' });
    y += 2; doc.setDrawColor(120); doc.line(L, y, R, y); y += 4;

    doc.setFont('helvetica', 'normal');
    for (const i of venta.items) {
      const nom = i.productoNombre.length > 32 ? i.productoNombre.substring(0, 30) + '..' : i.productoNombre;
      doc.text(nom, c1, y);
      doc.text(String(i.cantidad), c2, y, { align: 'right' });
      doc.text(`Bs.${i.precioUnitario.toFixed(2)}`, c3, y, { align: 'right' });
      doc.text(`Bs.${i.subtotalItem.toFixed(2)}`, c4, y, { align: 'right' });
      y += 5;
    }
    y += 1; hr();

    const tot = (label: string, value: string, bold = false) => {
      doc.setFont('helvetica', bold ? 'bold' : 'normal');
      doc.setFontSize(bold ? 11 : 9);
      doc.text(label, L, y);
      doc.text(value, R, y, { align: 'right' });
      y += bold ? 6 : 5;
    };
    tot('Subtotal:', `Bs. ${venta.subtotal.toFixed(2)}`);
    if (venta.descuentoCupon > 0) tot(`Cupon (${venta.codigoCupon}):`, `-Bs. ${venta.descuentoCupon.toFixed(2)}`);
    if (venta.descuentoFrecuente > 0) tot('Desc. frecuente:', `-Bs. ${venta.descuentoFrecuente.toFixed(2)}`);
    if (venta.descuentoManual > 0) tot('Desc. especial:', `-Bs. ${venta.descuentoManual.toFixed(2)}`);
    tot('TOTAL:', `Bs. ${venta.totalFinal.toFixed(2)}`, true);
    hr();

    doc.setFont('helvetica', 'normal'); doc.setFontSize(9);
    doc.text('¡Gracias por su compra!', W / 2, y, { align: 'center' });
    doc.save(`recibo-${venta.id}.pdf`);
  }

  imprimirReciboPedido(p: PedidoResponse): void {
    const html = this.buildReciboPedidoHtml(p);
    const w = window.open('', 'recibo', 'width=370,height=620,scrollbars=yes');
    if (!w) { this.error.set('Permite ventanas emergentes para generar el PDF'); return; }
    w.document.write(html); w.document.close(); w.focus();
    setTimeout(() => w.print(), 500);
  }

  generarMensajePedido(p: PedidoResponse): string {
    const fecha = new Date(p.fechaVenta as any).toLocaleString('es-BO', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
    const cliente = p.clienteNombre ? `👤 ${p.clienteNombre}${p.clienteCi ? ' · CI: ' + p.clienteCi : ''}\n` : '';
    const sep = '─────────────────────';
    let entrega = p.tipoEntrega === 'DOMICILIO'
      ? `🚚 *Entrega a domicilio*${p.direccionEntrega ? ': ' + p.direccionEntrega : ''}`
      : `🏪 *Retiro en tienda*`;
    return `🐾 *SpaMascotas — Pedido #${p.ventaId}*\n📅 ${fecha}\n${cliente}${entrega}\n${sep}\n${p.itemsResumen}\n${sep}\n*TOTAL: Bs. ${p.totalFinal.toFixed(2)}*\n\n✅ *Pedido entregado y pagado.* ¡Gracias por su compra! 🐶🐱`;
  }

  togglePromoProducto(id: number) {
    this.promoProductoIds.update(ids =>
      ids.includes(id) ? ids.filter(x => x !== id) : [...ids, id]
    );
  }

  toggleCuponProducto(id: number) {
    this.cuponProductoIds.update(ids =>
      ids.includes(id) ? ids.filter(x => x !== id) : [...ids, id]
    );
  }

  enviarPorWA() {
    const v = this.ventaCreada();
    if (!v) return;
    const telf = v.clienteTelefono ?? this.telefonoCheckout();
    if (!telf) return;
    this.svc.abrirWhatsApp(telf, this.svc.generarMensajeVenta(v, this.isRecepcion()));
  }

  descargarReciboPedido(p: PedidoResponse): void {
    const doc = new jsPDF({ format: 'a5', unit: 'mm' });
    const W = doc.internal.pageSize.getWidth();
    const H = doc.internal.pageSize.getHeight();
    const L = 12, R = W - 12;

    // Fondo oscuro
    doc.setFillColor(18, 14, 35);
    doc.rect(0, 0, W, H, 'F');

    // Franja header
    doc.setFillColor(28, 10, 60);
    doc.rect(0, 0, W, 34, 'F');
    doc.setDrawColor(124, 58, 237); doc.setLineWidth(0.5);
    doc.line(0, 34, W, 34);

    // Título
    doc.setFontSize(17); doc.setTextColor(196, 181, 253); doc.setFont('helvetica', 'bold');
    doc.text('SpaMascotas', W / 2, 14, { align: 'center' });
    doc.setFontSize(9); doc.setTextColor(148, 163, 184); doc.setFont('helvetica', 'normal');
    doc.text('Recibo de compra · ENTREGADO', W / 2, 22, { align: 'center' });

    const fechaVenta = new Date(p.fechaVenta as any).toLocaleString('es-BO', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
    doc.setFontSize(8); doc.setTextColor(100, 116, 139);
    doc.text(fechaVenta, W / 2, 29, { align: 'center' });

    // Datos cliente
    let y = 42;
    const fila = (label: string, val: string, destacado = false) => {
      doc.setFillColor(26, 21, 48);
      doc.roundedRect(L, y - 4, R - L, 8, 1.5, 1.5, 'F');
      doc.setFontSize(8.5); doc.setTextColor(107, 114, 128); doc.setFont('helvetica', 'normal');
      doc.text(label, L + 3, y + 0.5);
      doc.setTextColor(destacado ? 167 : 226, destacado ? 139 : 232, destacado ? 250 : 240);
      doc.setFont('helvetica', 'bold');
      doc.text(val, R - 3, y + 0.5, { align: 'right' });
      y += 10;
    };

    fila('N° Pedido', `#${p.ventaId}`);
    fila('Cliente', p.clienteNombre ?? '—');
    if (p.clienteCi)       fila('CI', p.clienteCi);
    if (p.clienteTelefono) fila('Teléfono', p.clienteTelefono);
    fila('Entrega', p.tipoEntrega === 'DOMICILIO' ? 'Domicilio' : 'Retiro en tienda');
    if (p.direccionEntrega) fila('Dirección', p.direccionEntrega);
    if (p.fechaEntregaPedido) {
      const fechaEntrega = new Date(p.fechaEntregaPedido as any).toLocaleString('es-BO', {
        day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
      });
      fila('Entregado', fechaEntrega);
    }

    // Separador
    y += 2;
    doc.setDrawColor(124, 58, 237); doc.setLineWidth(0.3);
    doc.line(L, y, R, y); y += 6;

    // Productos
    doc.setFontSize(7.5); doc.setTextColor(148, 163, 184); doc.setFont('helvetica', 'bold');
    doc.text('PRODUCTOS', L, y); y += 5;

    const lineas = doc.splitTextToSize(p.itemsResumen, R - L - 4);
    doc.setFont('helvetica', 'normal'); doc.setTextColor(203, 213, 225); doc.setFontSize(8.5);
    doc.text(lineas, L + 2, y);
    y += lineas.length * 5 + 4;

    // Separador total
    doc.setDrawColor(124, 58, 237); doc.line(L, y, R, y); y += 7;

    // Total
    doc.setFillColor(124, 58, 237, 0.15 as any);
    doc.setFillColor(40, 20, 80);
    doc.roundedRect(L, y - 5, R - L, 11, 2, 2, 'F');
    doc.setFontSize(11); doc.setTextColor(167, 139, 250); doc.setFont('helvetica', 'bold');
    doc.text('TOTAL', L + 4, y + 2);
    doc.setFontSize(13); doc.setTextColor(196, 181, 253);
    doc.text(`Bs. ${p.totalFinal.toFixed(2)}`, R - 4, y + 2, { align: 'right' });
    y += 16;

    // Pie
    doc.setFontSize(8); doc.setTextColor(100, 116, 139); doc.setFont('helvetica', 'italic');
    doc.text('¡Gracias por tu compra en SpaMascotas!', W / 2, y, { align: 'center' });

    doc.save(`recibo-pedido-${p.ventaId}.pdf`);
  }

  openProductoModal(p?: ProductoResponse) {
    this.editingProducto.set(p ?? null);
    this.selectedImage.set(null);
    this.selectedImageName.set('');
    if (p) {
      this.productoForm.patchValue({
        nombre: p.nombre, descripcion: p.descripcion ?? '',
        sku: p.sku ?? '', precioVenta: p.precioVenta,
        stockActual: p.stockActual, stockMinimo: p.stockMinimo,
        fechaVencimiento: p.fechaVencimiento ?? '', lote: p.lote ?? '',
        categoriaId: p.categoriaId ?? '',
      });
    } else {
      this.productoForm.reset({ stockActual: 0, stockMinimo: 0 });
    }
    this.showProductoModal.set(true);
  }

  closeProductoModal() { this.showProductoModal.set(false); this.editingProducto.set(null); }

  onImageSelected(e: Event) {
    const input = e.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedImage.set(input.files[0]);
      this.selectedImageName.set(input.files[0].name);
    }
  }

  submitProducto() {
    if (this.productoForm.invalid) return;
    const v = this.productoForm.value;
    const req = {
      nombre: v.nombre, descripcion: v.descripcion || undefined,
      sku: v.sku || undefined, precioVenta: v.precioVenta,
      stockActual: v.stockActual, stockMinimo: v.stockMinimo,
      fechaVencimiento: v.fechaVencimiento || undefined,
      lote: v.lote || undefined,
      categoriaId: v.categoriaId || undefined,
    };
    this.saving.set(true);
    const editing = this.editingProducto();
    const obs = editing
      ? this.svc.actualizarProducto(editing.id, req)
      : this.svc.crearProducto(req);

    obs.subscribe({
      next: prod => {
        const img = this.selectedImage();
        if (img) {
          this.svc.subirImagenProducto(prod.id, img).subscribe({
            next: () => { this.afterSaveProducto(editing); },
            error: () => { this.afterSaveProducto(editing); this.error.set('Producto guardado pero la imagen no pudo subirse'); },
          });
        } else {
          this.afterSaveProducto(editing);
        }
      },
      error: e => { this.error.set(this.apiErr(e) ?? 'Error al guardar producto'); this.saving.set(false); },
    });
  }

  private afterSaveProducto(editing: ProductoResponse | null) {
    this.saving.set(false);
    this.closeProductoModal();
    this.loadProductosAdmin();
    this.showSuccess(editing ? 'Producto actualizado' : 'Producto creado');
  }

  async toggleProducto(p: ProductoResponse) {
    const ok = await this.confirm.confirm({
      title: `${p.activo ? 'Desactivar' : 'Activar'} producto`,
      message: `¿Seguro que deseas ${p.activo ? 'desactivar' : 'activar'} "${p.nombre}"?`,
      confirmLabel: p.activo ? 'Desactivar' : 'Activar',
      danger: p.activo,
    });
    if (!ok) return;
    this.svc.toggleProducto(p.id).subscribe({
      next: () => { this.loadProductosAdmin(); this.showSuccess(`Producto ${p.activo ? 'desactivado' : 'activado'}`); },
      error: () => this.error.set('Error al cambiar estado del producto'),
    });
  }

  openCategoriaModal(c?: CategoriaResponse) {
    this.editingCategoria.set(c ?? null);
    this.categoriaForm.reset(c ? { nombre: c.nombre, descripcion: c.descripcion ?? '' } : {});
    this.showCategoriaModal.set(true);
  }

  closeCategoriaModal() { this.showCategoriaModal.set(false); this.editingCategoria.set(null); }

  submitCategoria() {
    if (this.categoriaForm.invalid) return;
    const v = this.categoriaForm.value;
    const editing = this.editingCategoria();
    const obs = editing
      ? this.svc.actualizarCategoria(editing.id, v)
      : this.svc.crearCategoria(v);
    this.saving.set(true);
    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeCategoriaModal();
        this.svc.getCategoriasAdmin().subscribe({ next: c => this.categorias.set(c) });
        this.showSuccess(editing ? 'Categoría actualizada' : 'Categoría creada');
      },
      error: e => { this.error.set(this.apiErr(e) ?? 'Error'); this.saving.set(false); },
    });
  }

  async toggleCategoria(c: CategoriaResponse) {
    const ok = await this.confirm.confirm({
      title: `${c.activa ? 'Desactivar' : 'Activar'} categoría`,
      message: `¿${c.activa ? 'Desactivar' : 'Activar'} "${c.nombre}"?`,
      confirmLabel: c.activa ? 'Desactivar' : 'Activar', danger: c.activa,
    });
    if (!ok) return;
    this.svc.toggleCategoria(c.id).subscribe({
      next: () => this.svc.getCategoriasAdmin().subscribe({ next: d => this.categorias.set(d) }),
    });
  }

  openPromoModal(p?: PromocionResponse) {
    this.editingPromo.set(p ?? null);
    this.promoProductoIds.set(p?.productoIds ?? []);
    this.promoProductoSearch.set('');
    this.promoForm.reset(p ? {
      nombre: p.nombre, descripcion: p.descripcion ?? '',
      tipo: p.tipo, descuentoPorcentaje: p.descuentoPorcentaje,
      fechaInicio: p.fechaInicio, fechaFin: p.fechaFin,
    } : { tipo: 'TEMPORADA', descuentoPorcentaje: 0 });
    this.showPromoModal.set(true);
  }

  closePromoModal() { this.showPromoModal.set(false); this.editingPromo.set(null); }

  submitPromo() {
    if (this.promoForm.invalid) return;
    const v = this.promoForm.value;
    const ids = this.promoProductoIds();
    const req = { ...v, productoIds: ids.length > 0 ? ids : [] };
    const editing = this.editingPromo();
    const obs = editing
      ? this.svc.actualizarPromocion(editing.id, req)
      : this.svc.crearPromocion(req);
    this.saving.set(true);
    obs.subscribe({
      next: () => { this.saving.set(false); this.closePromoModal(); this.loadPromociones(); this.showSuccess(editing ? 'Promoción actualizada' : 'Promoción creada'); },
      error: e => { this.error.set(this.apiErr(e) ?? 'Error'); this.saving.set(false); },
    });
  }

  async eliminarPromo(p: PromocionResponse) {
    const ok = await this.confirm.confirm({ title: 'Eliminar promoción', message: `¿Eliminar "${p.nombre}"?`, confirmLabel: 'Eliminar', danger: true });
    if (!ok) return;
    this.svc.eliminarPromocion(p.id).subscribe({
      next: () => { this.loadPromociones(); this.showSuccess('Promoción eliminada'); },
    });
  }

  openCuponModal(c?: CuponResponse) {
    this.editingCupon.set(c ?? null);
    this.cuponProductoIds.set(c?.productoIds ?? []);
    this.cuponProductoSearch.set('');
    this.cuponForm.reset(c ? {
      codigo: c.codigo, descripcion: c.descripcion ?? '',
      descuentoPorcentaje: c.descuentoPorcentaje,
      descuentoFijo: c.descuentoFijo, usosMax: c.usosMax ?? '',
      fechaVencimiento: c.fechaVencimiento ?? '',
    } : { descuentoPorcentaje: 0, descuentoFijo: 0 });
    this.showCuponModal.set(true);
  }

  closeCuponModal() { this.showCuponModal.set(false); this.editingCupon.set(null); }

  submitCupon() {
    if (this.cuponForm.invalid) return;
    const v = this.cuponForm.value;
    const ids = this.cuponProductoIds();
    const req = {
      codigo: v.codigo, descripcion: v.descripcion || undefined,
      descuentoPorcentaje: v.descuentoPorcentaje || 0,
      descuentoFijo: v.descuentoFijo || 0,
      usosMax: v.usosMax || undefined,
      fechaVencimiento: v.fechaVencimiento || undefined,
      productoIds: ids.length > 0 ? ids : undefined,
    };
    const editing = this.editingCupon();
    const obs = editing ? this.svc.actualizarCupon(editing.id, req) : this.svc.crearCupon(req);
    this.saving.set(true);
    obs.subscribe({
      next: () => { this.saving.set(false); this.closeCuponModal(); this.loadCupones(); this.showSuccess(editing ? 'Cupón actualizado' : 'Cupón creado'); },
      error: e => { this.error.set(this.apiErr(e) ?? 'Error'); this.saving.set(false); },
    });
  }

  async toggleCupon(c: CuponResponse) {
    const ok = await this.confirm.confirm({
      title: `${c.activo ? 'Desactivar' : 'Activar'} cupón`,
      message: `¿${c.activo ? 'Desactivar' : 'Activar'} el cupón "${c.codigo}"?`,
      confirmLabel: c.activo ? 'Desactivar' : 'Activar', danger: c.activo,
    });
    if (!ok) return;
    this.svc.toggleCupon(c.id).subscribe({
      next: () => { this.loadCupones(); this.showSuccess(`Cupón ${c.activo ? 'desactivado' : 'activado'}`); },
    });
  }

  private buildForms() {
    this.productoForm = this.fb.group({
      nombre:          ['', Validators.required],
      descripcion:     [''],
      sku:             [''],
      precioVenta:     [null, [Validators.required, Validators.min(0)]],
      stockActual:     [0, Validators.min(0)],
      stockMinimo:     [0, Validators.min(0)],
      fechaVencimiento:[''],
      lote:            [''],
      categoriaId:     [''],
    });

    this.categoriaForm = this.fb.group({
      nombre:      ['', Validators.required],
      descripcion: [''],
    });

    this.promoForm = this.fb.group({
      nombre:              ['', Validators.required],
      descripcion:         [''],
      tipo:                ['TEMPORADA', Validators.required],
      descuentoPorcentaje: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
      fechaInicio:         ['', Validators.required],
      fechaFin:            ['', Validators.required],
    });

    this.cuponForm = this.fb.group({
      codigo:              ['', Validators.required],
      descripcion:         [''],
      descuentoPorcentaje: [0, [Validators.min(0), Validators.max(100)]],
      descuentoFijo:       [0, Validators.min(0)],
      usosMax:             [''],
      fechaVencimiento:    [''],
    });
  }

  private showSuccess(msg: string, ms = 3000) {
    this.success.set(msg);
    setTimeout(() => this.success.set(null), ms);
  }

  dismissError() { this.error.set(null); }

  private apiErr(e: any): string | null {
    return e?.error?.mensaje ?? e?.error?.message ?? null;
  }

  stockClass(p: ProductoResponse): string {
    if (p.stockActual === 0) return 'stock-out';
    if (p.stockActual <= p.stockMinimo) return 'stock-low';
    return 'stock-ok';
  }

  esVencido(p: ProductoResponse): boolean {
    if (!p.fechaVencimiento) return false;
    const hoy = new Date();
    const hoyLocal = `${hoy.getFullYear()}-${String(hoy.getMonth()+1).padStart(2,'0')}-${String(hoy.getDate()).padStart(2,'0')}`;
    return p.fechaVencimiento <= hoyLocal;
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    return new Date(d + 'T12:00:00').toLocaleDateString('es-BO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatDateTime(dt?: string): string {
    if (!dt) return '—';
    const d = new Date(dt);
    return d.toLocaleDateString('es-BO', { day: '2-digit', month: 'short', year: 'numeric' })
      + ' ' + d.toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit' });
  }

  tipoLabel(t: string): string {
    const map: Record<string, string> = {
      TEMPORADA: 'Temporada', CAMPANA: 'Campaña',
      CLIENTE_FRECUENTE: 'Cliente frecuente', OTRO: 'Otro',
    };
    return map[t] ?? t;
  }

  estadoPedidoClass(estado?: string): string {
    const map: Record<string, string> = {
      EN_ESPERA: 'badge-espera', ENTREGADO: 'badge-entregado', CANCELADO: 'badge-cancelado',
    };
    return map[estado ?? ''] ?? 'badge-estado';
  }

  inCart(id: number): number {
    return this.cart().find(i => i.producto.id === id)?.cantidad ?? 0;
  }

  categoriaName(id?: number): string {
    return this.categorias().find(c => c.id === id)?.nombre ?? '—';
  }

  productoImagen(p: ProductoResponse): string {
    return p.urlImagen ? FILE_BASE + p.urlImagen : '';
  }

  itemImagen(urlImagen?: string): string {
    return urlImagen ? FILE_BASE + urlImagen : '';
  }
}
