package com.spamascotas.spa_mascotas_api.controller.agenda;

import com.spamascotas.spa_mascotas_api.dto.request.BloqueoRequest;
import com.spamascotas.spa_mascotas_api.dto.request.HorarioTrabajoRequest;
import com.spamascotas.spa_mascotas_api.dto.response.BloqueoResponse;
import com.spamascotas.spa_mascotas_api.dto.response.GroomerResponse;
import com.spamascotas.spa_mascotas_api.dto.response.HorarioTrabajoResponse;
import com.spamascotas.spa_mascotas_api.service.agenda.DisponibilidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agenda")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
public class DisponibilidadController {

    private final DisponibilidadService service;

    @GetMapping("/groomers")
    public List<GroomerResponse> listarGroomers() {
        return service.listarGroomers();
    }

    @GetMapping("/groomers/{id}/horarios")
    public List<HorarioTrabajoResponse> horariosPorGroomer(@PathVariable Long id) {
        return service.horariosPorEmpleado(id);
    }

    @PostMapping("/horarios")
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioTrabajoResponse crearHorario(@Valid @RequestBody HorarioTrabajoRequest req) {
        return service.crearHorario(req);
    }

    @PutMapping("/horarios/{id}")
    public HorarioTrabajoResponse actualizarHorario(@PathVariable Long id,
            @Valid @RequestBody HorarioTrabajoRequest req) {
        return service.actualizarHorario(id, req);
    }

    @DeleteMapping("/horarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarHorario(@PathVariable Long id) {
        service.eliminarHorario(id);
    }

    @GetMapping("/bloqueos")
    public List<BloqueoResponse> listarBloqueos() {
        return service.listarBloqueos();
    }

    @PostMapping("/bloqueos")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueoResponse crearBloqueo(@Valid @RequestBody BloqueoRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return service.crearBloqueo(req, user.getUsername());
    }

    @PutMapping("/bloqueos/{id}")
    public BloqueoResponse actualizarBloqueo(@PathVariable Long id,
            @Valid @RequestBody BloqueoRequest req) {
        return service.actualizarBloqueo(id, req);
    }

    @DeleteMapping("/bloqueos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBloqueo(@PathVariable Long id) {
        service.eliminarBloqueo(id);
    }
}
