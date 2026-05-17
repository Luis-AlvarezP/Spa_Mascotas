package com.spamascotas.spa_mascotas_api.service.agenda;

import com.spamascotas.spa_mascotas_api.dto.request.BloqueoRequest;
import com.spamascotas.spa_mascotas_api.dto.request.HorarioTrabajoRequest;
import com.spamascotas.spa_mascotas_api.dto.response.BloqueoResponse;
import com.spamascotas.spa_mascotas_api.dto.response.GroomerResponse;
import com.spamascotas.spa_mascotas_api.dto.response.HorarioTrabajoResponse;
import com.spamascotas.spa_mascotas_api.model.BloqueoAgenda;
import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.model.HorarioTrabajo;
import com.spamascotas.spa_mascotas_api.model.Usuario;
import com.spamascotas.spa_mascotas_api.repository.BloqueoAgendaRepository;
import com.spamascotas.spa_mascotas_api.repository.EmpleadoRepository;
import com.spamascotas.spa_mascotas_api.repository.HorarioTrabajoRepository;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final EmpleadoRepository empleadoRepo;
    private final HorarioTrabajoRepository horarioRepo;
    private final BloqueoAgendaRepository bloqueoRepo;
    private final UsuarioRepository usuarioRepo;

    @Transactional(readOnly = true)
    public List<GroomerResponse> listarGroomers() {
        return empleadoRepo.findByPuesto("GROOMER").stream()
                .map(e -> GroomerResponse.builder()
                        .id(e.getId())
                        .nombre(e.getNombre())
                        .correo(e.getUsuario() != null ? e.getUsuario().getCorreo() : null)
                        .activo(e.isActivo())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HorarioTrabajoResponse> horariosPorEmpleado(Long empleadoId) {
        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        return horarioRepo.findByEmpleadoOrderByDiaSemana(emp).stream()
                .map(this::toHorarioResponse)
                .toList();
    }

    @Transactional
    public HorarioTrabajoResponse crearHorario(HorarioTrabajoRequest req) {
        Empleado emp = empleadoRepo.findById(req.getEmpleadoId())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        HorarioTrabajo h = HorarioTrabajo.builder()
                .empleado(emp)
                .diaSemana(req.getDiaSemana())
                .horaInicio(LocalTime.parse(req.getHoraInicio()))
                .horaFin(LocalTime.parse(req.getHoraFin()))
                .inicioAlmuerzo(req.getInicioAlmuerzo() != null && !req.getInicioAlmuerzo().isBlank()
                        ? LocalTime.parse(req.getInicioAlmuerzo()) : null)
                .finAlmuerzo(req.getFinAlmuerzo() != null && !req.getFinAlmuerzo().isBlank()
                        ? LocalTime.parse(req.getFinAlmuerzo()) : null)
                .vigenteDesde(LocalDate.parse(req.getVigenteDesde()))
                .vigenteHasta(req.getVigenteHasta() != null && !req.getVigenteHasta().isBlank()
                        ? LocalDate.parse(req.getVigenteHasta()) : null)
                .capacidadMaxima(req.getCapacidadMaxima())
                .build();

        return toHorarioResponse(horarioRepo.save(h));
    }

    @Transactional
    public HorarioTrabajoResponse actualizarHorario(Long id, HorarioTrabajoRequest req) {
        HorarioTrabajo h = horarioRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        Empleado emp = empleadoRepo.findById(req.getEmpleadoId())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        h.setEmpleado(emp);
        h.setDiaSemana(req.getDiaSemana());
        h.setHoraInicio(LocalTime.parse(req.getHoraInicio()));
        h.setHoraFin(LocalTime.parse(req.getHoraFin()));
        h.setInicioAlmuerzo(req.getInicioAlmuerzo() != null && !req.getInicioAlmuerzo().isBlank()
                ? LocalTime.parse(req.getInicioAlmuerzo()) : null);
        h.setFinAlmuerzo(req.getFinAlmuerzo() != null && !req.getFinAlmuerzo().isBlank()
                ? LocalTime.parse(req.getFinAlmuerzo()) : null);
        h.setVigenteDesde(LocalDate.parse(req.getVigenteDesde()));
        h.setVigenteHasta(req.getVigenteHasta() != null && !req.getVigenteHasta().isBlank()
                ? LocalDate.parse(req.getVigenteHasta()) : null);
        h.setCapacidadMaxima(req.getCapacidadMaxima());

        return toHorarioResponse(horarioRepo.save(h));
    }

    @Transactional
    public void eliminarHorario(Long id) {
        if (!horarioRepo.existsById(id)) {
            throw new RuntimeException("Horario no encontrado");
        }
        horarioRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<BloqueoResponse> listarBloqueos() {
        return bloqueoRepo.findAllByOrderByFechaInicioDesc().stream()
                .map(this::toBloqueoResponse)
                .toList();
    }

    @Transactional
    public BloqueoResponse crearBloqueo(BloqueoRequest req, String correoUsuario) {
        Empleado emp = req.getEmpleadoId() != null
                ? empleadoRepo.findById(req.getEmpleadoId()).orElse(null)
                : null;

        Usuario creador = usuarioRepo.findByCorreo(correoUsuario).orElse(null);

        BloqueoAgenda b = BloqueoAgenda.builder()
                .titulo(req.getTitulo())
                .tipo(req.getTipo())
                .fechaInicio(LocalDateTime.parse(req.getFechaInicio()))
                .fechaFin(LocalDateTime.parse(req.getFechaFin()))
                .empleado(emp)
                .descripcion(req.getDescripcion())
                .creadoPor(creador)
                .build();

        return toBloqueoResponse(bloqueoRepo.save(b));
    }

    @Transactional
    public BloqueoResponse actualizarBloqueo(Long id, BloqueoRequest req) {
        BloqueoAgenda b = bloqueoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloqueo no encontrado"));

        Empleado emp = req.getEmpleadoId() != null
                ? empleadoRepo.findById(req.getEmpleadoId()).orElse(null)
                : null;

        b.setTitulo(req.getTitulo());
        b.setTipo(req.getTipo());
        b.setFechaInicio(LocalDateTime.parse(req.getFechaInicio()));
        b.setFechaFin(LocalDateTime.parse(req.getFechaFin()));
        b.setEmpleado(emp);
        b.setDescripcion(req.getDescripcion());

        return toBloqueoResponse(bloqueoRepo.save(b));
    }

    @Transactional
    public void eliminarBloqueo(Long id) {
        if (!bloqueoRepo.existsById(id)) {
            throw new RuntimeException("Bloqueo no encontrado");
        }
        bloqueoRepo.deleteById(id);
    }

    private HorarioTrabajoResponse toHorarioResponse(HorarioTrabajo h) {
        return HorarioTrabajoResponse.builder()
                .id(h.getId())
                .empleadoId(h.getEmpleado() != null ? h.getEmpleado().getId() : null)
                .empleadoNombre(h.getEmpleado() != null ? h.getEmpleado().getNombre() : null)
                .diaSemana(h.getDiaSemana())
                .horaInicio(h.getHoraInicio())
                .horaFin(h.getHoraFin())
                .inicioAlmuerzo(h.getInicioAlmuerzo())
                .finAlmuerzo(h.getFinAlmuerzo())
                .vigenteDesde(h.getVigenteDesde())
                .vigenteHasta(h.getVigenteHasta())
                .capacidadMaxima(h.getCapacidadMaxima() != null ? h.getCapacidadMaxima() : 8)
                .build();
    }

    private BloqueoResponse toBloqueoResponse(BloqueoAgenda b) {
        return BloqueoResponse.builder()
                .id(b.getId())
                .titulo(b.getTitulo())
                .tipo(b.getTipo())
                .fechaInicio(b.getFechaInicio())
                .fechaFin(b.getFechaFin())
                .empleadoId(b.getEmpleado() != null ? b.getEmpleado().getId() : null)
                .empleadoNombre(b.getEmpleado() != null ? b.getEmpleado().getNombre() : null)
                .descripcion(b.getDescripcion())
                .creadoPorCorreo(b.getCreadoPor() != null ? b.getCreadoPor().getCorreo() : null)
                .build();
    }
}
