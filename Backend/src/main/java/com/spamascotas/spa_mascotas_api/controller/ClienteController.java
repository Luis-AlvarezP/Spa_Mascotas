package com.spamascotas.spa_mascotas_api.controller;

import com.spamascotas.spa_mascotas_api.dto.response.ClienteAdminResponse;
import com.spamascotas.spa_mascotas_api.service.admin.AdminClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final AdminClienteService adminClienteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION','GROOMER')")
    public ResponseEntity<List<ClienteAdminResponse>> listar() {
        return ResponseEntity.ok(adminClienteService.listarClientes());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteAdminResponse> toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(adminClienteService.toggleEstadoCliente(id, user.getUsername()));
    }
}
