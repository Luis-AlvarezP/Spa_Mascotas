package com.spamascotas.spa_mascotas_api.controller.inventario;

import com.spamascotas.spa_mascotas_api.dto.request.*;
import com.spamascotas.spa_mascotas_api.dto.response.*;
import com.spamascotas.spa_mascotas_api.service.inventario.InventarioAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventario")
@RequiredArgsConstructor
public class InventarioAdminController {

    private final InventarioAdminService service;

    

    @GetMapping("/productos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<ProductoResponse> listarProductos() {
        return service.listarTodosProductos();
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPCION')")
    public List<ProductoResponse> listarStockBajo() {
        return service.listarProductosBajoStock();
    }

    @PostMapping("/productos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest req) {
        return service.crearProducto(req);
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ProductoResponse actualizar(@PathVariable("id") Long id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizarProducto(id, req);
    }

    @PatchMapping("/productos/{id}/toggle")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ProductoResponse toggle(@PathVariable("id") Long id) {
        return service.toggleProducto(id);
    }

    @PostMapping(value = "/productos/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ProductoResponse subirImagen(@PathVariable("id") Long id,
                                        @RequestParam("file") MultipartFile file) {
        return service.subirImagen(id, file);
    }

    

    @GetMapping("/categorias")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<CategoriaResponse> listarCategorias() {
        return service.listarTodasCategorias();
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CategoriaResponse crearCategoria(@Valid @RequestBody CategoriaRequest req) {
        return service.crearCategoria(req);
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CategoriaResponse actualizarCategoria(@PathVariable("id") Long id,
                                                  @Valid @RequestBody CategoriaRequest req) {
        return service.actualizarCategoria(id, req);
    }

    @PatchMapping("/categorias/{id}/toggle")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CategoriaResponse toggleCategoria(@PathVariable("id") Long id) {
        return service.toggleCategoria(id);
    }



    @GetMapping("/promociones")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<PromocionResponse> listarPromociones() {
        return service.listarPromociones();
    }

    @PostMapping("/promociones")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PromocionResponse crearPromocion(@Valid @RequestBody PromocionRequest req) {
        return service.crearPromocion(req);
    }

    @PutMapping("/promociones/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PromocionResponse actualizarPromocion(@PathVariable("id") Long id,
                                                  @Valid @RequestBody PromocionRequest req) {
        return service.actualizarPromocion(id, req);
    }

    @DeleteMapping("/promociones/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void eliminarPromocion(@PathVariable("id") Long id) {
        service.eliminarPromocion(id);
    }


    @GetMapping("/cupones")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<CuponResponse> listarCupones() {
        return service.listarCupones();
    }

    @PostMapping("/cupones")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CuponResponse crearCupon(@Valid @RequestBody CuponRequest req) {
        return service.crearCupon(req);
    }

    @PutMapping("/cupones/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CuponResponse actualizarCupon(@PathVariable("id") Long id,
                                          @Valid @RequestBody CuponRequest req) {
        return service.actualizarCupon(id, req);
    }

    @PatchMapping("/cupones/{id}/toggle")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CuponResponse toggleCupon(@PathVariable("id") Long id) {
        return service.toggleCupon(id);
    }
}
