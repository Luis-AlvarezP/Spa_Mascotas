package com.spamascotas.spa_mascotas_api.controller.grooming;

import com.spamascotas.spa_mascotas_api.dto.response.HorarioTrabajoResponse;
import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.repository.EmpleadoRepository;
import com.spamascotas.spa_mascotas_api.repository.HorarioTrabajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grooming")
@RequiredArgsConstructor
public class GroomingController {

    private final EmpleadoRepository empleadoRepo;
    private final HorarioTrabajoRepository horarioRepo;

    @GetMapping("/mis-horarios")
    @PreAuthorize("hasAuthority('ROLE_GROOMER')")
    public List<HorarioTrabajoResponse> misHorarios(@AuthenticationPrincipal UserDetails user) {
        Empleado emp = empleadoRepo.findByUsuarioCorreo(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Perfil de empleado no encontrado"));
        return horarioRepo.findByEmpleadoOrderByDiaSemana(emp).stream()
                .map(h -> HorarioTrabajoResponse.builder()
                        .id(h.getId())
                        .empleadoId(h.getEmpleado().getId())
                        .empleadoNombre(h.getEmpleado().getNombre())
                        .diaSemana(h.getDiaSemana())
                        .horaInicio(h.getHoraInicio())
                        .horaFin(h.getHoraFin())
                        .inicioAlmuerzo(h.getInicioAlmuerzo())
                        .finAlmuerzo(h.getFinAlmuerzo())
                        .vigenteDesde(h.getVigenteDesde())
                        .vigenteHasta(h.getVigenteHasta())
                        .capacidadMaxima(h.getCapacidadMaxima() != null ? h.getCapacidadMaxima() : 8)
                        .build())
                .toList();
    }
}
