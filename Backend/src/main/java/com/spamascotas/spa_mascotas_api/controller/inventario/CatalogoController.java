package com.spamascotas.spa_mascotas_api.controller.inventario;

import com.spamascotas.spa_mascotas_api.dto.response.*;
import com.spamascotas.spa_mascotas_api.service.inventario.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/productos")
    public List<ProductoResponse> productos() {
        return catalogoService.listarProductos();
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return catalogoService.listarCategorias();
    }

    @GetMapping("/promociones")
    public List<PromocionResponse> promociones() {
        return catalogoService.listarPromocionesActivas();
    }

    @GetMapping("/metodos-pago")
    public List<MetodoPagoResponse> metodosPago() {
        return catalogoService.listarMetodosPago();
    }

    @PostMapping("/validar-cupon")
    public CuponValidadoResponse validarCupon(@RequestBody Map<String, Object> body) {
        String codigo = (String) body.get("codigo");
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("productoIds");
        List<Long> productoIds = ids != null
            ? ids.stream().map(Integer::longValue).toList()
            : List.of();
        double sub = body.get("subtotal") instanceof Number n ? n.doubleValue() : 0.0;
        return catalogoService.validarCupon(codigo, productoIds, BigDecimal.valueOf(sub));
    }
}
