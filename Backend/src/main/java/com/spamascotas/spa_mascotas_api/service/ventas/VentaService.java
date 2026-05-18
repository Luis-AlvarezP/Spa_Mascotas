package com.spamascotas.spa_mascotas_api.service.ventas;

import com.spamascotas.spa_mascotas_api.dto.request.VentaRequest;
import com.spamascotas.spa_mascotas_api.dto.response.PedidoResponse;
import com.spamascotas.spa_mascotas_api.dto.response.VentaItemResponse;
import com.spamascotas.spa_mascotas_api.dto.response.VentaResponse;
import com.spamascotas.spa_mascotas_api.model.*;
import com.spamascotas.spa_mascotas_api.repository.*;
import com.spamascotas.spa_mascotas_api.service.admin.AuditService;
import com.spamascotas.spa_mascotas_api.model.enums.TipoAccion;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaService {

    private static final int FRECUENTE_MAX_PCT = 5;

    private final VentaRepository ventaRepository;
    private final VentaItemRepository ventaItemRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ProductoRepository productoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final CuponRepository cuponRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final PedidoRepository pedidoRepository;
    private final AuditService auditService;

    @Transactional
    public VentaResponse crearVenta(VentaRequest req, String correoUsuario, boolean esRecepcion) {
        // Resolver cliente (crea registro lazy si aún no existe)
        Cliente cliente;
        if (esRecepcion && req.getClienteId() != null) {
            cliente = clienteRepository.findById(req.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        } else {
            cliente = clienteRepository.findByUsuarioCorreo(correoUsuario)
                .orElseGet(() -> {
                    Usuario u = usuarioRepository.findByCorreo(correoUsuario)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                    return clienteRepository.save(Cliente.builder()
                        .usuario(u)
                        .nombre(u.getNombreUsuario() != null ? u.getNombreUsuario() : correoUsuario)
                        .build());
                });
        }

        // Actualizar teléfono si se proveyó y no tiene
        if (req.getTelefonoContacto() != null && !req.getTelefonoContacto().isBlank()
                && (cliente.getTelefono() == null || cliente.getTelefono().isBlank())) {
            cliente.setTelefono(req.getTelefonoContacto());
            clienteRepository.save(cliente);
        }

        MetodoPago metodoPago = metodoPagoRepository.findById(req.getMetodoPagoId())
            .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

        // Resolver vendedor (solo para RECEPCION)
        Empleado vendedor = null;
        if (esRecepcion) {
            vendedor = empleadoRepository.findByUsuarioCorreo(correoUsuario).orElse(null);
        }

        // Validar ítems y calcular subtotal
        List<Producto> productos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : req.getItems()) {
            Producto p = productoRepository.findById(item.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getProductoId()));
            if (!Boolean.TRUE.equals(p.getActivo()))
                throw new RuntimeException("Producto inactivo: " + p.getNombre());
            if (p.getStockActual() < item.getCantidad())
                throw new RuntimeException("Stock insuficiente para: " + p.getNombre());
            productos.add(p);
            subtotal = subtotal.add(p.getPrecioVenta().multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        // Descuento cliente frecuente: 1% por cada 10 pedidos, máximo 5%
        long comprasAnteriores = ventaRepository.countByClienteIdAndEstado(cliente.getId(), "CONFIRMADA");
        int descuentoPct = (int) Math.min(FRECUENTE_MAX_PCT, comprasAnteriores / 10);
        boolean esFrecuente = descuentoPct > 0;
        BigDecimal descuentoFrecuente = descuentoPct > 0
            ? subtotal.multiply(BigDecimal.valueOf(descuentoPct)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Descuento cupón
        Cupon cupon = null;
        BigDecimal descuentoCupon = BigDecimal.ZERO;
        if (req.getCodigoCupon() != null && !req.getCodigoCupon().isBlank()) {
            cupon = cuponRepository.findByCodigoIgnoreCase(req.getCodigoCupon())
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
            validarCuponUso(cupon);
            List<Long> idsProductos = req.getItems().stream()
                .map(i -> i.getProductoId()).collect(Collectors.toList());
            descuentoCupon = calcularDescuentoCupon(cupon, idsProductos, subtotal);
        }

        // Descuento manual (solo RECEPCION)
        BigDecimal descuentoManual = BigDecimal.ZERO;
        if (esRecepcion && req.getDescuentoManual() != null && req.getDescuentoManual().compareTo(BigDecimal.ZERO) > 0) {
            descuentoManual = req.getDescuentoManual();
        }

        BigDecimal totalDescuento = descuentoCupon.add(descuentoFrecuente).add(descuentoManual);
        BigDecimal totalFinal = subtotal.subtract(totalDescuento).max(BigDecimal.ZERO);

        // Crear venta
        Venta venta = Venta.builder()
            .cliente(cliente).vendedor(vendedor).metodoPago(metodoPago).cupon(cupon)
            .subtotal(subtotal).descuento(totalDescuento)
            .descuentoFrecuente(descuentoFrecuente).descuentoManual(descuentoManual)
            .totalFinal(totalFinal).estado("CONFIRMADA")
            .build();
        venta = ventaRepository.save(venta);

        // Crear ítems + descontar stock + registrar movimientos
        List<VentaItem> ventaItems = new ArrayList<>();
        for (int i = 0; i < req.getItems().size(); i++) {
            var itemReq = req.getItems().get(i);
            Producto p = productos.get(i);
            VentaItem vi = ventaItemRepository.save(VentaItem.builder()
                .venta(venta).producto(p)
                .cantidad(itemReq.getCantidad())
                .precioUnitarioHistorico(p.getPrecioVenta())
                .build());
            ventaItems.add(vi);
            p.setStockActual(p.getStockActual() - itemReq.getCantidad());
            productoRepository.save(p);
            movimientoRepository.save(MovimientoInventario.builder()
                .producto(p).empleado(vendedor)
                .tipoMovimiento("VENTA")
                .cantidad(-itemReq.getCantidad())
                .notas("Venta #" + venta.getId())
                .build());
        }
        venta.setItems(ventaItems);

        // Incrementar usos del cupón
        if (cupon != null) {
            cupon.setUsosActuales(cupon.getUsosActuales() + 1);
            cuponRepository.save(cupon);
        }

        // Crear pedido asociado a la venta
        // Recepción = venta en persona → entrega inmediata; Cliente = queda EN_ESPERA
        String tipoEntrega = esRecepcion ? "RECOGER"
            : (req.getTipoEntrega() != null && !req.getTipoEntrega().isBlank() ? req.getTipoEntrega() : "RECOGER");
        String estadoPedido = esRecepcion ? "ENTREGADO" : "EN_ESPERA";
        Pedido pedido = pedidoRepository.save(Pedido.builder()
            .venta(venta).tipoEntrega(tipoEntrega)
            .direccionEntrega(esRecepcion ? null : req.getDireccionEntrega())
            .estado(estadoPedido)
            .fechaEntregaPedido(esRecepcion ? LocalDateTime.now() : null)
            .build());
        venta.setPedido(pedido);

        auditService.registrar(TipoAccion.VENTA_REALIZADA, correoUsuario,
            esRecepcion ? "RECEPCION" : "CLIENTE", true,
            "Venta #" + venta.getId() + " | Total: " + totalFinal + " | Cliente: " + cliente.getNombre());

        return toResponse(venta, esFrecuente, descuentoCupon);
    }

    @Transactional
    public List<VentaResponse> misPedidos(String correo) {
        Cliente cliente = clienteRepository.findByUsuarioCorreo(correo)
            .orElseGet(() -> {
                Usuario u = usuarioRepository.findByCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                return clienteRepository.save(Cliente.builder()
                    .usuario(u)
                    .nombre(u.getNombreUsuario() != null ? u.getNombreUsuario() : correo)
                    .build());
            });
        return ventaRepository.findByClienteIdOrderByFechaVentaDesc(cliente.getId()).stream()
            .map(v -> toResponse(v, false, BigDecimal.ZERO))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> todasVentas() {
        return ventaRepository.findAllByOrderByFechaVentaDesc().stream()
            .map(v -> toResponse(v, false, BigDecimal.ZERO))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPedidos() {
        return pedidoRepository.findAllByOrderByIdDesc().stream()
            .map(this::toPedidoResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public PedidoResponse entregarPedido(Long pedidoId) {
        Pedido p = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        if (!"EN_ESPERA".equals(p.getEstado()))
            throw new RuntimeException("El pedido no está en espera");
        p.setEstado("ENTREGADO");
        p.setFechaEntregaPedido(LocalDateTime.now());
        return toPedidoResponse(pedidoRepository.save(p));
    }

    @Transactional
    public PedidoResponse cancelarPedido(Long pedidoId, String correoUsuario, boolean esStaff) {
        Pedido p = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        if (!"EN_ESPERA".equals(p.getEstado()))
            throw new RuntimeException("Solo se puede cancelar un pedido en espera");
        if (!esStaff) {
            String correoCliente = p.getVenta().getCliente().getUsuario().getCorreo();
            if (!correoCliente.equals(correoUsuario))
                throw new RuntimeException("No puedes cancelar este pedido");
        }
        p.setEstado("CANCELADO");
        for (VentaItem item : p.getVenta().getItems()) {
            Producto prod = item.getProducto();
            prod.setStockActual(prod.getStockActual() + item.getCantidad());
            productoRepository.save(prod);
        }
        return toPedidoResponse(pedidoRepository.save(p));
    }

    private void validarCuponUso(Cupon c) {
        if (!Boolean.TRUE.equals(c.getActivo())) throw new RuntimeException("Cupón inactivo");
        if (c.getFechaVencimiento() != null && c.getFechaVencimiento().isBefore(LocalDate.now()))
            throw new RuntimeException("Cupón expirado");
        if (c.getUsosMax() != null && c.getUsosActuales() >= c.getUsosMax())
            throw new RuntimeException("Cupón sin usos disponibles");
    }

    private BigDecimal calcularDescuentoCupon(Cupon cupon, List<Long> idsItems, BigDecimal subtotal) {
        if (cupon.getDescuentoPorcentaje().compareTo(BigDecimal.ZERO) > 0) {
            return subtotal.multiply(cupon.getDescuentoPorcentaje())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return cupon.getDescuentoFijo().min(subtotal);
    }

    private VentaResponse toResponse(Venta v, boolean esFrecuente, BigDecimal descuentoCupon) {
        var canal = v.getCliente() != null ? getCanal(v.getCliente()) : null;
        List<VentaItemResponse> items = v.getItems().stream()
            .map(i -> VentaItemResponse.builder()
                .id(i.getId())
                .productoId(i.getProducto().getId())
                .productoNombre(i.getProducto().getNombre())
                .productoUrlImagen(i.getProducto().getUrlImagen())
                .cantidad(i.getCantidad())
                .precioUnitario(i.getPrecioUnitarioHistorico())
                .subtotalItem(i.getPrecioUnitarioHistorico().multiply(BigDecimal.valueOf(i.getCantidad())))
                .build())
            .collect(Collectors.toList());

        Pedido pedido = v.getPedido();
        return VentaResponse.builder()
            .id(v.getId())
            .clienteId(v.getCliente() != null ? v.getCliente().getId() : null)
            .clienteNombre(v.getCliente() != null ? v.getCliente().getNombre() : null)
            .clienteCi(v.getCliente() != null ? v.getCliente().getCi() : null)
            .clienteTelefono(v.getCliente() != null ? v.getCliente().getTelefono() : null)
            .clienteCanal(canal)
            .vendedorNombre(v.getVendedor() != null ? v.getVendedor().getNombre() : null)
            .metodoPago(v.getMetodoPago() != null ? v.getMetodoPago().getNombre() : null)
            .codigoCupon(v.getCupon() != null ? v.getCupon().getCodigo() : null)
            .fechaVenta(v.getFechaVenta()).subtotal(v.getSubtotal())
            .descuentoCupon(descuentoCupon).descuentoFrecuente(v.getDescuentoFrecuente())
            .descuentoManual(v.getDescuentoManual()).descuento(v.getDescuento())
            .totalFinal(v.getTotalFinal()).estado(v.getEstado()).notas(v.getNotas())
            .clienteFrecuente(esFrecuente).items(items)
            .pedidoId(pedido != null ? pedido.getId() : null)
            .estadoPedido(pedido != null ? pedido.getEstado() : null)
            .tipoEntrega(pedido != null ? pedido.getTipoEntrega() : null)
            .direccionEntrega(pedido != null ? pedido.getDireccionEntrega() : null)
            .build();
    }

    private PedidoResponse toPedidoResponse(Pedido p) {
        Venta v = p.getVenta();
        String itemsResumen = v.getItems().stream()
            .map(i -> i.getProducto().getNombre() + " ×" + i.getCantidad())
            .collect(Collectors.joining(", "));
        return PedidoResponse.builder()
            .id(p.getId()).ventaId(v.getId())
            .tipoEntrega(p.getTipoEntrega()).direccionEntrega(p.getDireccionEntrega())
            .estado(p.getEstado()).fechaEntregaPedido(p.getFechaEntregaPedido())
            .fechaVenta(v.getFechaVenta())
            .clienteNombre(v.getCliente() != null ? v.getCliente().getNombre() : null)
            .clienteCi(v.getCliente() != null ? v.getCliente().getCi() : null)
            .clienteTelefono(v.getCliente() != null ? v.getCliente().getTelefono() : null)
            .totalFinal(v.getTotalFinal()).itemsResumen(itemsResumen)
            .build();
    }

    private String getCanal(Cliente c) {
        if (c.getUsuario() == null) return null;
        // Canal guardado en cliente_preferencias con nombre='canal_preferido'
        return null; // se obtiene en el frontend desde el perfil del cliente
    }
}
