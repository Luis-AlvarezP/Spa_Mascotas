package com.spamascotas.spa_mascotas_api.controller.auth;

import com.spamascotas.spa_mascotas_api.dto.request.Enable2faRequest;
import com.spamascotas.spa_mascotas_api.dto.request.ForgotPasswordRequest;
import com.spamascotas.spa_mascotas_api.dto.request.Init2faRequest;
import com.spamascotas.spa_mascotas_api.dto.request.LoginRequest;
import com.spamascotas.spa_mascotas_api.dto.request.RefreshTokenRequest;
import com.spamascotas.spa_mascotas_api.dto.request.RegisterRequest;
import com.spamascotas.spa_mascotas_api.dto.request.ResetPasswordRequest;
import com.spamascotas.spa_mascotas_api.dto.response.AuthResponse;
import com.spamascotas.spa_mascotas_api.dto.response.MessageResponse;
import com.spamascotas.spa_mascotas_api.dto.response.Setup2faResponse;
import com.spamascotas.spa_mascotas_api.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registrar(request));
    }

    @GetMapping("/activar")
    public ResponseEntity<MessageResponse> activar(@RequestParam String token) {
        return ResponseEntity.ok(authService.activarCuenta(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }



    @PostMapping("/2fa/init")
    public ResponseEntity<Setup2faResponse> init2fa(@Valid @RequestBody Init2faRequest request) {
        return ResponseEntity.ok(authService.init2faSetup(request.getCorreo(), request.getPassword()));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<AuthResponse> enable2fa(@Valid @RequestBody Enable2faRequest request) {
        return ResponseEntity.ok(authService.enable2fa(request.getCorreo(), request.getPassword(), request.getCodigo()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.solicitarReset(req.getIdentificador()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(authService.resetearPassword(req.getToken(), req.getNuevaPassword()));
    }
}
