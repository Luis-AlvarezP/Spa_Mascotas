package com.spamascotas.spa_mascotas_api.service.inventario;

import com.spamascotas.spa_mascotas_api.dto.response.*;
import com.spamascotas.spa_mascotas_api.model.Cupon;
import com.spamascotas.spa_mascotas_api.model.Producto;
import com.spamascotas.spa_mascotas_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spamascotas.spa_mascotas_api.model.Promocion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PromocionRepository promocionRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final CuponRepository cuponRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductos() {
        List<Promocion> activas = promocionRepository.findActivas(LocalDate.now());
        return productoRepository.findByActivoTrue().stream()
            .map(p -> toProductoResponse(p, activas))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findByActivaTrue().stream()
            .map(c -> CategoriaResponse.builder()
                .id(c.getId()).nombre(c.getNombre())
                .descripcion(c.getDescripcion()).activa(c.getActiva())
                .build())
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromocionResponse> listarPromocionesActivas() {
        return promocionRepository.findActivas(LocalDate.now()).stream()
            .map(p -> PromocionResponse.builder()
                .id(p.getId()).nombre(p.getNombre()).descripcion(p.getDescripcion())
                .tipo(p.getTipo()).descuentoPorcentaje(p.getDescuentoPorcentaje())
                .fechaInicio(p.getFechaInicio()).fechaFin(p.getFechaFin()).activa(p.getActiva())
                .build())
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoResponse> listarMetodosPago() {
        return metodoPagoRepository.findAll().stream()
            .map(m -> MetodoPagoResponse.builder().id(m.getId()).nombre(m.getNombre()).build())
            .collect(Collectors.toList());
    }

    @Transactional
    public CuponValidadoResponse validarCupon(String codigo, List<Long> productoIds, BigDecimal subtotal) {
        Cupon cupon = cuponRepository.findByCodigoIgnoreCase(codigo)
            .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));

        if (!Boolean.TRUE.equals(cupon.getActivo()))
            throw new RuntimeException("El cupón está inactivo");
        if (cupon.getFechaVencimiento() != null && cupon.getFechaVencimiento().isBefore(LocalDate.now()))
            throw new RuntimeException("El cupón ha expirado");
        if (cupon.getUsosMax() != null && cupon.getUsosActuales() >= cupon.getUsosMax())
            throw new RuntimeException("El cupón alcanzó el límite de usos");

        boolean aplicaATodo = cupon.getProductos().isEmpty();
        BigDecimal base = subtotal;

        if (!aplicaATodo && productoIds != null) {
            List<Long> idsProductosCupon = cupon.getProductos().stream()
                .map(Producto::getId).collect(Collectors.toList());
            // El descuento se calcula sobre los ítems que aplica; aquí retornamos el monto sobre subtotal para simplificar
            boolean tieneCoincidencia = productoIds.stream().anyMatch(idsProductosCupon::contains);
            if (!tieneCoincidencia) throw new RuntimeException("El cupón no aplica a los productos seleccionados");
        }

        BigDecimal monto;
        if (cupon.getDescuentoPorcentaje().compareTo(BigDecimal.ZERO) > 0) {
            monto = base.multiply(cupon.getDescuentoPorcentaje()).divide(BigDecimal.valueOf(100));
        } else {
            monto = cupon.getDescuentoFijo().min(base);
        }

        return CuponValidadoResponse.builder()
            .cuponId(cupon.getId()).codigo(cupon.getCodigo()).descripcion(cupon.getDescripcion())
            .descuentoPorcentaje(cupon.getDescuentoPorcentaje()).descuentoFijo(cupon.getDescuentoFijo())
            .montoDescuento(monto).aplicaATodo(aplicaATodo)
            .build();
    }

    public ProductoResponse toProductoResponse(Producto p) {
        return toProductoResponse(p, List.of());
    }

    public ProductoResponse toProductoResponse(Producto p, List<Promocion> activas) {
        int descuentoPct = activas.stream()
            .filter(pr -> pr.getProductos().isEmpty()
                || pr.getProductos().stream().anyMatch(pp -> pp.getId().equals(p.getId())))
            .mapToInt(pr -> pr.getDescuentoPorcentaje().intValue())
            .max()
            .orElse(0);
        BigDecimal precioFinal = descuentoPct > 0
            ? p.getPrecioVenta()
                .multiply(BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(descuentoPct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP)
            : p.getPrecioVenta();
        return ProductoResponse.builder()
            .id(p.getId()).nombre(p.getNombre()).descripcion(p.getDescripcion())
            .sku(p.getSku()).stockActual(p.getStockActual()).stockMinimo(p.getStockMinimo())
            .precioVenta(p.getPrecioVenta()).precioFinal(precioFinal).descuentoPct(descuentoPct)
            .fechaVencimiento(p.getFechaVencimiento())
            .lote(p.getLote()).urlImagen(p.getUrlImagen()).activo(p.getActivo())
            .categoriaId(p.getCategoria() != null ? p.getCategoria().getId() : null)
            .categoriaNombre(p.getCategoria() != null ? p.getCategoria().getNombre() : null)
            .build();
    }
}
