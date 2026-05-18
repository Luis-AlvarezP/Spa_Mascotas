package com.spamascotas.spa_mascotas_api.controller.mascotas;

import com.spamascotas.spa_mascotas_api.dto.request.MascotaRequest;
import com.spamascotas.spa_mascotas_api.dto.response.MascotaResponse;
import com.spamascotas.spa_mascotas_api.dto.response.TemperamentoResponse;
import com.spamascotas.spa_mascotas_api.service.mascotas.MascotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService service;

    @GetMapping("/temperamentos")
    @PreAuthorize("isAuthenticated()")
    public List<TemperamentoResponse> temperamentos() {
        return service.listarTemperamentos();
    }

    // ── CLIENTE endpoints ────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public List<MascotaResponse> misMascotas(@AuthenticationPrincipal UserDetails user) {
        return service.misMascotas(user.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public MascotaResponse registrar(@Valid @RequestBody MascotaRequest req,
                                     @AuthenticationPrincipal UserDetails user) {
        return service.registrar(req, user.getUsername());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public MascotaResponse actualizar(@PathVariable("id") Long id,
                                      @Valid @RequestBody MascotaRequest req,
                                      @AuthenticationPrincipal UserDetails user) {
        return service.actualizar(id, req, user.getUsername());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public MascotaResponse toggleActiva(@PathVariable("id") Long id,
                                        @AuthenticationPrincipal UserDetails user) {
        return service.toggleActiva(id, user.getUsername());
    }

    @PostMapping("/{id}/carnet")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public ResponseEntity<Map<String, String>> subirCarnet(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) {
        String url = service.subirCarnet(id, file, user.getUsername());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/{id}/foto")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public ResponseEntity<Map<String, String>> subirFoto(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) {
        String url = service.subirFoto(id, file, user.getUsername());
        return ResponseEntity.ok(Map.of("url", url));
    }

    // ── STAFF endpoint ───────────────────────────────────────────

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCION','ROLE_GROOMER')")
    public List<MascotaResponse> mascotasByCliente(@PathVariable("clienteId") Long clienteId) {
        return service.mascotasByCliente(clienteId);
    }
}
