package com.spamascotas.spa_mascotas_api.controller.ventas;

import com.spamascotas.spa_mascotas_api.dto.request.VentaRequest;
import com.spamascotas.spa_mascotas_api.dto.response.VentaResponse;
import com.spamascotas.spa_mascotas_api.service.ventas.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENTE','ROLE_RECEPCION','ROLE_ADMIN')")
    public VentaResponse crear(@Valid @RequestBody VentaRequest req,
                               @AuthenticationPrincipal UserDetails user) {
        boolean esRecepcion = user.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_RECEPCION") || a.getAuthority().equals("ROLE_ADMIN"));
        return ventaService.crearVenta(req, user.getUsername(), esRecepcion);
    }

    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public List<VentaResponse> misPedidos(@AuthenticationPrincipal UserDetails user) {
        return ventaService.misPedidos(user.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCION')")
    public List<VentaResponse> todas() {
        return ventaService.todasVentas();
    }
}
