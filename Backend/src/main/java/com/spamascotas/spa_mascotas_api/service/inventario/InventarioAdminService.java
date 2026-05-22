package com.spamascotas.spa_mascotas_api.service.inventario;

import com.spamascotas.spa_mascotas_api.dto.request.*;
import com.spamascotas.spa_mascotas_api.dto.response.*;
import com.spamascotas.spa_mascotas_api.model.*;
import com.spamascotas.spa_mascotas_api.model.enums.TipoAccion;
import com.spamascotas.spa_mascotas_api.repository.*;
import com.spamascotas.spa_mascotas_api.service.admin.AuditService;
import com.spamascotas.spa_mascotas_api.service.sse.StockEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventarioAdminService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PromocionRepository promocionRepository;
    private final CuponRepository cuponRepository;
    private final CatalogoService catalogoService;
    private final StockEventService stockEventService;
    private final AuditService auditService;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    // ── Productos ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodosProductos() {
        return productoRepository.findAll().stream()
            .map(catalogoService::toProductoResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductosBajoStock() {
        return productoRepository.findByStockActualLessThanEqualAndActivoTrue(5)
            .stream().map(catalogoService::toProductoResponse).collect(Collectors.toList());
    }

    @Transactional
    public ProductoResponse crearProducto(ProductoRequest req) {
        Producto p = Producto.builder()
            .nombre(req.getNombre()).descripcion(req.getDescripcion())
            .sku(req.getSku()).precioVenta(req.getPrecioVenta())
            .stockActual(req.getStockActual() != null ? req.getStockActual() : 0)
            .stockMinimo(req.getStockMinimo() != null ? req.getStockMinimo() : 0)
            .fechaVencimiento(req.getFechaVencimiento()).lote(req.getLote())
            .activo(true)
            .build();
        if (req.getCategoriaId() != null) {
            categoriaRepository.findById(req.getCategoriaId()).ifPresent(p::setCategoria);
        }
        ProductoResponse resp = catalogoService.toProductoResponse(productoRepository.save(p));
        auditService.registrar(TipoAccion.CREAR_PRODUCTO, adminCorreo(), "ADMIN", true,
            "Producto: " + req.getNombre() + " | SKU: " + nvl(req.getSku()) + " | Stock: " + (req.getStockActual() != null ? req.getStockActual() : 0));
        stockEventService.notifyStockChange();
        return resp;
    }

    @Transactional
    public ProductoResponse actualizarProducto(Long id, ProductoRequest req) {
        Producto p = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        StringBuilder cambios = new StringBuilder("Producto: " + req.getNombre());
        if (!java.util.Objects.equals(p.getNombre(), req.getNombre()))
            cambios.append(" | Nombre: ").append(p.getNombre()).append(" → ").append(req.getNombre());
        if (req.getStockActual() != null && !java.util.Objects.equals(p.getStockActual(), req.getStockActual()))
            cambios.append(" | Stock: ").append(p.getStockActual()).append(" → ").append(req.getStockActual());
        if (req.getStockMinimo() != null && !java.util.Objects.equals(p.getStockMinimo(), req.getStockMinimo()))
            cambios.append(" | Stock mín.: ").append(p.getStockMinimo()).append(" → ").append(req.getStockMinimo());
        if (req.getPrecioVenta() != null && req.getPrecioVenta().compareTo(p.getPrecioVenta() != null ? p.getPrecioVenta() : java.math.BigDecimal.ZERO) != 0)
            cambios.append(" | Precio: ").append(p.getPrecioVenta()).append(" → ").append(req.getPrecioVenta());
        if (!java.util.Objects.equals(p.getSku(), req.getSku()))
            cambios.append(" | SKU: ").append(nvl(p.getSku())).append(" → ").append(nvl(req.getSku()));

        p.setNombre(req.getNombre());
        p.setDescripcion(req.getDescripcion());
        p.setSku(req.getSku());
        p.setPrecioVenta(req.getPrecioVenta());
        if (req.getStockActual() != null) p.setStockActual(req.getStockActual());
        if (req.getStockMinimo() != null) p.setStockMinimo(req.getStockMinimo());
        p.setFechaVencimiento(req.getFechaVencimiento());
        p.setLote(req.getLote());
        if (req.getCategoriaId() != null) {
            categoriaRepository.findById(req.getCategoriaId()).ifPresent(p::setCategoria);
        } else {
            p.setCategoria(null);
        }
        ProductoResponse resp = catalogoService.toProductoResponse(productoRepository.save(p));
        auditService.registrar(TipoAccion.MODIFICAR_PRODUCTO, adminCorreo(), "ADMIN", true, cambios.toString());
        stockEventService.notifyStockChange();
        return resp;
    }

    @Transactional
    public ProductoResponse toggleProducto(Long id) {
        Producto p = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        boolean nuevoEstado = !Boolean.TRUE.equals(p.getActivo());
        p.setActivo(nuevoEstado);
        ProductoResponse resp = catalogoService.toProductoResponse(productoRepository.save(p));
        TipoAccion accion = nuevoEstado ? TipoAccion.MODIFICAR_PRODUCTO : TipoAccion.DESACTIVAR_PRODUCTO;
        auditService.registrar(accion, adminCorreo(), "ADMIN", true,
            "Producto: " + p.getNombre() + " | Estado: " + (nuevoEstado ? "Activado" : "Desactivado"));
        stockEventService.notifyStockChange();
        return resp;
    }

    @Transactional
    public ProductoResponse subirImagen(Long id, MultipartFile file) {
        Producto p = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        String url = saveFile(file, "productos");
        p.setUrlImagen(url);
        return catalogoService.toProductoResponse(productoRepository.save(p));
    }

    // ── Categorías ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodasCategorias() {
        return categoriaRepository.findAll().stream()
            .map(c -> CategoriaResponse.builder()
                .id(c.getId()).nombre(c.getNombre())
                .descripcion(c.getDescripcion()).activa(c.getActiva())
                .build())
            .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaResponse crearCategoria(CategoriaRequest req) {
        Categoria c = Categoria.builder()
            .nombre(req.getNombre()).descripcion(req.getDescripcion()).activa(true)
            .build();
        c = categoriaRepository.save(c);
        return CategoriaResponse.builder().id(c.getId()).nombre(c.getNombre())
            .descripcion(c.getDescripcion()).activa(c.getActiva()).build();
    }

    @Transactional
    public CategoriaResponse actualizarCategoria(Long id, CategoriaRequest req) {
        Categoria c = categoriaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        c.setNombre(req.getNombre());
        c.setDescripcion(req.getDescripcion());
        c = categoriaRepository.save(c);
        return CategoriaResponse.builder().id(c.getId()).nombre(c.getNombre())
            .descripcion(c.getDescripcion()).activa(c.getActiva()).build();
    }

    @Transactional
    public CategoriaResponse toggleCategoria(Long id) {
        Categoria c = categoriaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        c.setActiva(!Boolean.TRUE.equals(c.getActiva()));
        c = categoriaRepository.save(c);
        return CategoriaResponse.builder().id(c.getId()).nombre(c.getNombre())
            .descripcion(c.getDescripcion()).activa(c.getActiva()).build();
    }

    // ── Promociones ──────────────────────────────────────────────

    @Transactional
    public List<PromocionResponse> listarPromociones() {
        return promocionRepository.findAll().stream()
            .map(this::toPromocionResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public PromocionResponse crearPromocion(PromocionRequest req) {
        Promocion p = Promocion.builder()
            .nombre(req.getNombre()).descripcion(req.getDescripcion()).tipo(req.getTipo())
            .descuentoPorcentaje(req.getDescuentoPorcentaje())
            .fechaInicio(req.getFechaInicio()).fechaFin(req.getFechaFin()).activa(true)
            .build();
        if (req.getProductoIds() != null && !req.getProductoIds().isEmpty()) {
            p.setProductos(new HashSet<>(productoRepository.findAllById(req.getProductoIds())));
        }
        p = promocionRepository.save(p);
        return toPromocionResponse(p);
    }

    @Transactional
    public PromocionResponse actualizarPromocion(Long id, PromocionRequest req) {
        Promocion p = promocionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Promoción no encontrada"));
        p.setNombre(req.getNombre()); p.setDescripcion(req.getDescripcion());
        p.setTipo(req.getTipo()); p.setDescuentoPorcentaje(req.getDescuentoPorcentaje());
        p.setFechaInicio(req.getFechaInicio()); p.setFechaFin(req.getFechaFin());
        if (req.getProductoIds() != null) {
            p.setProductos(req.getProductoIds().isEmpty()
                ? new HashSet<>()
                : new HashSet<>(productoRepository.findAllById(req.getProductoIds())));
        }
        return toPromocionResponse(promocionRepository.save(p));
    }

    @Transactional
    public void eliminarPromocion(Long id) {
        promocionRepository.deleteById(id);
    }

    // ── Cupones ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CuponResponse> listarCupones() {
        return cuponRepository.findAll().stream()
            .map(this::toCuponResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public CuponResponse crearCupon(CuponRequest req) {
        if (cuponRepository.findByCodigoIgnoreCase(req.getCodigo()).isPresent())
            throw new RuntimeException("Ya existe un cupón con ese código");
        Cupon c = Cupon.builder()
            .codigo(req.getCodigo().toUpperCase()).descripcion(req.getDescripcion())
            .descuentoPorcentaje(req.getDescuentoPorcentaje() != null ? req.getDescuentoPorcentaje() : java.math.BigDecimal.ZERO)
            .descuentoFijo(req.getDescuentoFijo() != null ? req.getDescuentoFijo() : java.math.BigDecimal.ZERO)
            .usosMax(req.getUsosMax()).fechaVencimiento(req.getFechaVencimiento()).activo(true)
            .build();
        if (req.getProductoIds() != null) {
            Set<Producto> prods = new HashSet<>(productoRepository.findAllById(req.getProductoIds()));
            c.setProductos(prods);
        }
        return toCuponResponse(cuponRepository.save(c));
    }

    @Transactional
    public CuponResponse actualizarCupon(Long id, CuponRequest req) {
        Cupon c = cuponRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
        c.setDescripcion(req.getDescripcion());
        if (req.getDescuentoPorcentaje() != null) c.setDescuentoPorcentaje(req.getDescuentoPorcentaje());
        if (req.getDescuentoFijo() != null) c.setDescuentoFijo(req.getDescuentoFijo());
        c.setUsosMax(req.getUsosMax()); c.setFechaVencimiento(req.getFechaVencimiento());
        if (req.getProductoIds() != null) {
            c.setProductos(new HashSet<>(productoRepository.findAllById(req.getProductoIds())));
        }
        return toCuponResponse(cuponRepository.save(c));
    }

    @Transactional
    public CuponResponse toggleCupon(Long id) {
        Cupon c = cuponRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
        c.setActivo(!Boolean.TRUE.equals(c.getActivo()));
        return toCuponResponse(cuponRepository.save(c));
    }

    // ── Helpers ──────────────────────────────────────────────────

    private PromocionResponse toPromocionResponse(Promocion p) {
        List<Long> ids = p.getProductos().stream().map(Producto::getId).collect(Collectors.toList());
        return PromocionResponse.builder()
            .id(p.getId()).nombre(p.getNombre()).descripcion(p.getDescripcion())
            .tipo(p.getTipo()).descuentoPorcentaje(p.getDescuentoPorcentaje())
            .fechaInicio(p.getFechaInicio()).fechaFin(p.getFechaFin()).activa(p.getActiva())
            .productoIds(ids).aplicaATodo(ids.isEmpty())
            .build();
    }

    private CuponResponse toCuponResponse(Cupon c) {
        List<Long> ids = c.getProductos().stream().map(Producto::getId).collect(Collectors.toList());
        return CuponResponse.builder()
            .id(c.getId()).codigo(c.getCodigo()).descripcion(c.getDescripcion())
            .descuentoPorcentaje(c.getDescuentoPorcentaje()).descuentoFijo(c.getDescuentoFijo())
            .usosMax(c.getUsosMax()).usosActuales(c.getUsosActuales())
            .fechaVencimiento(c.getFechaVencimiento()).activo(c.getActivo())
            .productoIds(ids)
            .build();
    }

    private String adminCorreo() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "desconocido";
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String saveFile(MultipartFile file, String subdir) {
        String original = file.getOriginalFilename();
        String baseName = (original != null) ? Paths.get(original).getFileName().toString() : "file";
        String filename = UUID.randomUUID() + "_" + baseName;
        var dir = Paths.get(uploadsDir, subdir).toAbsolutePath();
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar imagen: " + e.getMessage());
        }
        return "/api/files/" + subdir + "/" + filename;
    }
}
