package com.spamascotas.spa_mascotas_api.controller;

import com.spamascotas.spa_mascotas_api.dto.request.CambiarPasswordRequest;
import com.spamascotas.spa_mascotas_api.dto.request.PerfilUpdateRequest;
import com.spamascotas.spa_mascotas_api.dto.request.VerifyTotpRequest;
import com.spamascotas.spa_mascotas_api.dto.response.MessageResponse;
import com.spamascotas.spa_mascotas_api.dto.response.PerfilResponse;
import com.spamascotas.spa_mascotas_api.dto.response.Setup2faResponse;
import com.spamascotas.spa_mascotas_api.service.PerfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping
    public ResponseEntity<PerfilResponse> obtener(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(perfilService.obtenerPerfil(user.getUsername()));
    }

    @PutMapping
    public ResponseEntity<PerfilResponse> actualizar(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody PerfilUpdateRequest request) {
        return ResponseEntity.ok(perfilService.actualizarPerfil(user.getUsername(), request));
    }

    @GetMapping("/2fa/init")
    public ResponseEntity<Setup2faResponse> init2fa(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(perfilService.init2faLoggedIn(user.getUsername()));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<MessageResponse> enable2fa(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody VerifyTotpRequest request) {
        return ResponseEntity.ok(perfilService.enable2faLoggedIn(user.getUsername(), request.getCodigo()));
    }

    @DeleteMapping("/2fa")
    public ResponseEntity<MessageResponse> disable2fa(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam String password) {
        return ResponseEntity.ok(perfilService.disable2fa(user.getUsername(), password));
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<MessageResponse> cambiarPassword(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CambiarPasswordRequest request) {
        return ResponseEntity.ok(perfilService.cambiarPassword(
                user.getUsername(), request.getPasswordActual(), request.getNuevaPassword()));
    }
}
