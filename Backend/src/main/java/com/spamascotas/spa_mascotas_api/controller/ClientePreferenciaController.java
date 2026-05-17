package com.spamascotas.spa_mascotas_api.controller;

import com.spamascotas.spa_mascotas_api.dto.request.PreferenciaRequest;
import com.spamascotas.spa_mascotas_api.dto.response.PreferenciaResponse;
import com.spamascotas.spa_mascotas_api.service.ClientePreferenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfil/preferencias")
@RequiredArgsConstructor
public class ClientePreferenciaController {

    private final ClientePreferenciaService service;

    @GetMapping
    public ResponseEntity<List<PreferenciaResponse>> listar(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(service.listar(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<PreferenciaResponse> guardar(@AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody PreferenciaRequest request) {
        return ResponseEntity.ok(service.guardar(user.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        service.eliminar(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
