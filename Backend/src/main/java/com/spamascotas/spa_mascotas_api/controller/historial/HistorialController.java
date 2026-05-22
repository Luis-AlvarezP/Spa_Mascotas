package com.spamascotas.spa_mascotas_api.controller.historial;

import com.spamascotas.spa_mascotas_api.dto.request.CalificacionRequest;
import com.spamascotas.spa_mascotas_api.dto.response.CalificacionResponse;
import com.spamascotas.spa_mascotas_api.dto.response.HistorialCitaResponse;
import com.spamascotas.spa_mascotas_api.service.historial.HistorialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historial")
@RequiredArgsConstructor
public class HistorialController {

    private final HistorialService service;

    @GetMapping("/mascota/{mascotaId}")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public List<HistorialCitaResponse> porMascota(@PathVariable Long mascotaId,
                                                   @AuthenticationPrincipal UserDetails user) {
        return service.historialPorMascota(mascotaId, user.getUsername());
    }

    @GetMapping("/mis-servicios")
    @PreAuthorize("hasAuthority('ROLE_GROOMER')")
    public List<HistorialCitaResponse> misServicios(@AuthenticationPrincipal UserDetails user) {
        return service.historialGroomer(user.getUsername());
    }

    @PostMapping("/calificar")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public CalificacionResponse calificar(@Valid @RequestBody CalificacionRequest req,
                                          @AuthenticationPrincipal UserDetails user) {
        return service.calificar(req, user.getUsername());
    }
}
