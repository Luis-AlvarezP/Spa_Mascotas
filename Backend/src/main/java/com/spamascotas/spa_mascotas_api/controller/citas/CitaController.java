package com.spamascotas.spa_mascotas_api.controller.citas;

import com.spamascotas.spa_mascotas_api.dto.request.CancelacionRequest;
import com.spamascotas.spa_mascotas_api.dto.request.CancelacionStaffRequest;
import com.spamascotas.spa_mascotas_api.dto.request.CitaRequest;
import com.spamascotas.spa_mascotas_api.dto.request.CobrarRequest;
import com.spamascotas.spa_mascotas_api.dto.request.RechazarRequest;
import com.spamascotas.spa_mascotas_api.dto.request.ReprogramarRequest;
import com.spamascotas.spa_mascotas_api.dto.response.CitaResponse;
import com.spamascotas.spa_mascotas_api.dto.response.GroomerResponse;
import com.spamascotas.spa_mascotas_api.dto.response.SlotResponse;
import com.spamascotas.spa_mascotas_api.service.citas.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService service;

    @GetMapping("/groomers")
    @PreAuthorize("isAuthenticated()")
    public List<GroomerResponse> listarGroomers() {
        return service.listarGroomers();
    }

    @GetMapping("/slots")
    @PreAuthorize("isAuthenticated()")
    public List<SlotResponse> slots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam Long servicioId,
            @RequestParam Long mascotaId,
            @RequestParam(required = false) Long empleadoId,
            @RequestParam(required = false) Long excluirCitaId) {
        return service.slotsDisponibles(fecha, servicioId, mascotaId, empleadoId, excluirCitaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public CitaResponse solicitar(@Valid @RequestBody CitaRequest req,
                                  @AuthenticationPrincipal UserDetails user) {
        return service.solicitar(req, user.getUsername());
    }

    @GetMapping("/mis-citas")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public List<CitaResponse> misCitas(@AuthenticationPrincipal UserDetails user) {
        return service.misCitas(user.getUsername());
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public CitaResponse cancelar(@PathVariable Long id,
                                 @Valid @RequestBody CancelacionRequest req,
                                 @AuthenticationPrincipal UserDetails user) {
        return service.cancelar(id, req, user.getUsername());
    }

    @PatchMapping("/{id}/reprogramar")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public CitaResponse reprogramar(@PathVariable Long id,
                                    @Valid @RequestBody ReprogramarRequest req,
                                    @AuthenticationPrincipal UserDetails user) {
        return service.reprogramar(id, req, user.getUsername());
    }

    @GetMapping("/todas")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION') or hasAuthority('ROLE_ADMIN')")
    public List<CitaResponse> todas() {
        return service.todasLasCitas();
    }

    @GetMapping("/mis-servicios")
    @PreAuthorize("hasAuthority('ROLE_GROOMER')")
    public List<CitaResponse> misServicios(@AuthenticationPrincipal UserDetails user) {
        return service.misServicios(user.getUsername());
    }

    @PatchMapping("/{id}/aceptar")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION') or hasAuthority('ROLE_ADMIN')")
    public CitaResponse aceptar(@PathVariable Long id) {
        return service.aceptar(id);
    }

    @PatchMapping("/{id}/finalizar")
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN')")
    public CitaResponse finalizar(@PathVariable Long id) {
        return service.finalizarServicio(id);
    }

    @PatchMapping("/{id}/cobrar")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION') or hasAuthority('ROLE_ADMIN')")
    public CitaResponse cobrar(@PathVariable Long id, @Valid @RequestBody CobrarRequest req) {
        return service.cobrar(id, req);
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION') or hasAuthority('ROLE_ADMIN')")
    public CitaResponse rechazar(@PathVariable Long id, @Valid @RequestBody RechazarRequest req) {
        return service.rechazar(id, req);
    }

    @PatchMapping("/{id}/cancelar-staff")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION') or hasAuthority('ROLE_ADMIN')")
    public CitaResponse cancelarStaff(@PathVariable Long id, @RequestBody CancelacionStaffRequest req) {
        return service.cancelarPorStaff(id, req);
    }
}
