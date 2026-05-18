import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CategoriaResponse {
  id: number;
  nombre: string;
  descripcion?: string;
  activa: boolean;
}

export interface ProductoResponse {
  id: number;
  nombre: string;
  descripcion?: string;
  sku?: string;
  stockActual: number;
  stockMinimo: number;
  precioVenta: number;
  precioFinal: number;
  descuentoPct: number;
  fechaVencimiento?: string;
  lote?: string;
  urlImagen?: string;
  activo: boolean;
  categoriaId?: number;
  categoriaNombre?: string;
}

export interface PromocionResponse {
  id: number;
  nombre: string;
  descripcion?: string;
  tipo: string;
  descuentoPorcentaje: number;
  fechaInicio: string;
  fechaFin: string;
  activa: boolean;
  productoIds: number[];
  aplicaATodo: boolean;
}

export interface PedidoResponse {
  id: number;
  ventaId: number;
  tipoEntrega: string;
  direccionEntrega?: string;
  estado: string;
  fechaEntregaPedido?: string;
  fechaVenta: string;
  clienteNombre?: string;
  clienteCi?: string;
  clienteTelefono?: string;
  totalFinal: number;
  itemsResumen: string;
}

export interface CuponResponse {
  id: number;
  codigo: string;
  descripcion?: string;
  descuentoPorcentaje: number;
  descuentoFijo: number;
  usosMax?: number;
  usosActuales: number;
  fechaVencimiento?: string;
  activo: boolean;
  productoIds: number[];
}

export interface CuponValidadoResponse {
  cuponId: number;
  codigo: string;
  descripcion?: string;
  descuentoPorcentaje: number;
  descuentoFijo: number;
  montoDescuento: number;
  aplicaATodo: boolean;
}

export interface MetodoPagoResponse {
  id: number;
  nombre: string;
}

export interface VentaItemRequest {
  productoId: number;
  cantidad: number;
}

export interface VentaRequest {
  clienteId?: number;
  metodoPagoId: number;
  codigoCupon?: string;
  descuentoManual?: number;
  items: VentaItemRequest[];
  telefonoContacto?: string;
  tipoEntrega?: string;
  direccionEntrega?: string;
}

export interface VentaItemResponse {
  id: number;
  productoId: number;
  productoNombre: string;
  productoUrlImagen?: string;
  cantidad: number;
  precioUnitario: number;
  subtotalItem: number;
}

export interface VentaResponse {
  id: number;
  clienteId?: number;
  clienteNombre?: string;
  clienteCi?: string;
  clienteTelefono?: string;
  clienteCanal?: string;
  vendedorNombre?: string;
  metodoPago?: string;
  codigoCupon?: string;
  fechaVenta: string;
  subtotal: number;
  descuentoCupon: number;
  descuentoFrecuente: number;
  descuentoManual: number;
  descuento: number;
  totalFinal: number;
  estado: string;
  notas?: string;
  clienteFrecuente: boolean;
  items: VentaItemResponse[];
  pedidoId?: number;
  estadoPedido?: string;
  tipoEntrega?: string;
  direccionEntrega?: string;
}

export interface ProductoRequest {
  nombre: string;
  descripcion?: string;
  sku?: string;
  precioVenta: number;
  stockActual?: number;
  stockMinimo?: number;
  fechaVencimiento?: string;
  lote?: string;
  categoriaId?: number;
}

export interface CategoriaRequest {
  nombre: string;
  descripcion?: string;
}

export interface PromocionRequest {
  nombre: string;
  descripcion?: string;
  tipo: string;
  descuentoPorcentaje: number;
  fechaInicio: string;
  fechaFin: string;
  productoIds?: number[];
}

export interface CuponRequest {
  codigo: string;
  descripcion?: string;
  descuentoPorcentaje?: number;
  descuentoFijo?: number;
  usosMax?: number;
  fechaVencimiento?: string;
  productoIds?: number[];
}

export interface CartItem {
  producto: ProductoResponse;
  cantidad: number;
}

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private http = inject(HttpClient);
  private base  = `${environment.apiUrl}`;

  // ── Catálogo (todos los roles) ───────────────────────────────

  getProductos(): Observable<ProductoResponse[]> {
    return this.http.get<ProductoResponse[]>(`${this.base}/catalogo/productos`);
  }

  getCategorias(): Observable<CategoriaResponse[]> {
    return this.http.get<CategoriaResponse[]>(`${this.base}/catalogo/categorias`);
  }

  getMetodosPago(): Observable<MetodoPagoResponse[]> {
    return this.http.get<MetodoPagoResponse[]>(`${this.base}/catalogo/metodos-pago`);
  }

  validarCupon(codigo: string, productoIds: number[], subtotal: number): Observable<CuponValidadoResponse> {
    return this.http.post<CuponValidadoResponse>(`${this.base}/catalogo/validar-cupon`, { codigo, productoIds, subtotal });
  }

  // ── Ventas ────────────────────────────────────────────────────

  crearVenta(req: VentaRequest): Observable<VentaResponse> {
    return this.http.post<VentaResponse>(`${this.base}/ventas`, req);
  }

  misPedidos(): Observable<VentaResponse[]> {
    return this.http.get<VentaResponse[]>(`${this.base}/ventas/mis-pedidos`);
  }

  // ── Admin: Productos ─────────────────────────────────────────

  getProductosAdmin(): Observable<ProductoResponse[]> {
    return this.http.get<ProductoResponse[]>(`${this.base}/admin/inventario/productos`);
  }

  crearProducto(req: ProductoRequest): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(`${this.base}/admin/inventario/productos`, req);
  }

  actualizarProducto(id: number, req: ProductoRequest): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(`${this.base}/admin/inventario/productos/${id}`, req);
  }

  toggleProducto(id: number): Observable<ProductoResponse> {
    return this.http.patch<ProductoResponse>(`${this.base}/admin/inventario/productos/${id}/toggle`, {});
  }

  subirImagenProducto(id: number, file: File): Observable<ProductoResponse> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<ProductoResponse>(`${this.base}/admin/inventario/productos/${id}/imagen`, fd);
  }

  // ── Admin: Categorías ────────────────────────────────────────

  getCategoriasAdmin(): Observable<CategoriaResponse[]> {
    return this.http.get<CategoriaResponse[]>(`${this.base}/admin/inventario/categorias`);
  }

  crearCategoria(req: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(`${this.base}/admin/inventario/categorias`, req);
  }

  actualizarCategoria(id: number, req: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.put<CategoriaResponse>(`${this.base}/admin/inventario/categorias/${id}`, req);
  }

  toggleCategoria(id: number): Observable<CategoriaResponse> {
    return this.http.patch<CategoriaResponse>(`${this.base}/admin/inventario/categorias/${id}/toggle`, {});
  }

  // ── Admin: Promociones ───────────────────────────────────────

  getPromocionesAdmin(): Observable<PromocionResponse[]> {
    return this.http.get<PromocionResponse[]>(`${this.base}/admin/inventario/promociones`);
  }

  crearPromocion(req: PromocionRequest): Observable<PromocionResponse> {
    return this.http.post<PromocionResponse>(`${this.base}/admin/inventario/promociones`, req);
  }

  actualizarPromocion(id: number, req: PromocionRequest): Observable<PromocionResponse> {
    return this.http.put<PromocionResponse>(`${this.base}/admin/inventario/promociones/${id}`, req);
  }

  eliminarPromocion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/inventario/promociones/${id}`);
  }

  // ── Admin: Cupones ───────────────────────────────────────────

  getCuponesAdmin(): Observable<CuponResponse[]> {
    return this.http.get<CuponResponse[]>(`${this.base}/admin/inventario/cupones`);
  }

  crearCupon(req: CuponRequest): Observable<CuponResponse> {
    return this.http.post<CuponResponse>(`${this.base}/admin/inventario/cupones`, req);
  }

  actualizarCupon(id: number, req: CuponRequest): Observable<CuponResponse> {
    return this.http.put<CuponResponse>(`${this.base}/admin/inventario/cupones/${id}`, req);
  }

  toggleCupon(id: number): Observable<CuponResponse> {
    return this.http.patch<CuponResponse>(`${this.base}/admin/inventario/cupones/${id}/toggle`, {});
  }

  // ── Pedidos ───────────────────────────────────────────────────

  listarPedidosAdmin(): Observable<PedidoResponse[]> {
    return this.http.get<PedidoResponse[]>(`${this.base}/pedidos`);
  }

  entregarPedido(id: number): Observable<PedidoResponse> {
    return this.http.patch<PedidoResponse>(`${this.base}/pedidos/${id}/entregar`, {});
  }

  cancelarPedido(id: number): Observable<PedidoResponse> {
    return this.http.patch<PedidoResponse>(`${this.base}/pedidos/${id}/cancelar`, {});
  }

  // ── Helpers ───────────────────────────────────────────────────

  generarMensajeVenta(venta: VentaResponse, entregado = false): string {
    const fecha = new Date(venta.fechaVenta).toLocaleString('es-BO', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
    const sep = '─────────────────────';
    const cliente = venta.clienteNombre
      ? `👤 ${venta.clienteNombre}${venta.clienteCi ? ' · CI: ' + venta.clienteCi : ''}\n`
      : '';
    const entrega = venta.tipoEntrega === 'DOMICILIO'
      ? `🚚 *Entrega a domicilio*${venta.direccionEntrega ? ': ' + venta.direccionEntrega : ''}`
      : `🏪 *Retiro en tienda*`;
    const items = venta.items.map(i =>
      `▪ ${i.productoNombre}\n  ${i.cantidad} × Bs. ${i.precioUnitario.toFixed(2)} = Bs. ${i.subtotalItem.toFixed(2)}`
    ).join('\n');

    if (entregado) {
      // ── Mensaje de entrega ──────────────────────────────────────
      let msg = `✅ *SpaMascotas — Pedido entregado #${venta.id}*\n`;
      msg += `📅 ${fecha}\n`;
      msg += cliente;
      msg += `${sep}\n`;
      msg += `${entrega}\n`;
      msg += `${sep}\n`;
      msg += `*Productos:*\n${items}\n`;
      msg += `${sep}\n`;
      msg += `*TOTAL PAGADO: Bs. ${venta.totalFinal.toFixed(2)}*\n`;
      msg += `💳 ${venta.metodoPago ?? '—'}\n`;
      msg += `${sep}\n`;
      msg += `✅ *¡Tu pedido fue entregado y pagado!*\n`;
      msg += `¡Gracias por tu compra! 🐶🐱`;
      return msg;
    }

    // ── Mensaje de confirmación de compra (pendiente) ───────────
    let msg = `🛒 *SpaMascotas — Pedido confirmado #${venta.id}*\n`;
    msg += `📅 ${fecha}\n`;
    msg += cliente;
    msg += `${sep}\n`;
    msg += `${entrega}\n`;
    msg += `${sep}\n`;
    msg += `*Productos:*\n${items}\n`;
    msg += `${sep}\n`;
    if (venta.descuentoCupon > 0)    msg += `🏷 Cupón (${venta.codigoCupon}): −Bs. ${venta.descuentoCupon.toFixed(2)}\n`;
    if (venta.descuentoFrecuente > 0) msg += `⭐ Desc. frecuente: −Bs. ${venta.descuentoFrecuente.toFixed(2)}\n`;
    if (venta.descuentoManual > 0)   msg += `✂ Desc. especial: −Bs. ${venta.descuentoManual.toFixed(2)}\n`;
    msg += `*TOTAL: Bs. ${venta.totalFinal.toFixed(2)}*\n`;
    msg += `💳 ${venta.metodoPago ?? '—'}\n`;
    msg += `${sep}\n`;
    msg += `⏳ _Tu pedido está en proceso. Te avisaremos cuando esté listo._\n`;
    msg += `⚠ _Se cancelará automáticamente si no se procesa en 24 horas._\n`;
    msg += `¡Gracias por tu compra! 🐶🐱`;
    return msg;
  }

  abrirWhatsApp(telefono: string, mensaje: string): void {
    const phone = telefono.replace(/\D/g, '');
    window.open(`https://wa.me/591${phone}?text=${encodeURIComponent(mensaje)}`, '_blank');
  }

}
