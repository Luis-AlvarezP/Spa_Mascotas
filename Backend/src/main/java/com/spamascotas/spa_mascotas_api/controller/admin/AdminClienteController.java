package com.spamascotas.spa_mascotas_api.controller.admin;

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
@RequestMapping("/api/admin/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminClienteController {

    private final AdminClienteService service;

    @GetMapping
    public ResponseEntity<List<ClienteAdminResponse>> listar() {
        return ResponseEntity.ok(service.listarClientes());
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ClienteAdminResponse> toggle(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(service.toggleEstadoCliente(id, user.getUsername()));
    }
}
