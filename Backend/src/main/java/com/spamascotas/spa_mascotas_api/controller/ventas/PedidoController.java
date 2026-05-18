package com.spamascotas.spa_mascotas_api.controller.ventas;

import com.spamascotas.spa_mascotas_api.dto.response.PedidoResponse;
import com.spamascotas.spa_mascotas_api.service.ventas.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final VentaService ventaService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCION')")
    public List<PedidoResponse> listar() {
        return ventaService.listarPedidos();
    }

    @PatchMapping("/{id}/entregar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCION')")
    public PedidoResponse entregar(@PathVariable("id") Long id) {
        return ventaService.entregarPedido(id);
    }

    @PatchMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable("id") Long id,
                                    @AuthenticationPrincipal UserDetails user) {
        boolean esStaff = user.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPCION"));
        return ventaService.cancelarPedido(id, user.getUsername(), esStaff);
    }
}
