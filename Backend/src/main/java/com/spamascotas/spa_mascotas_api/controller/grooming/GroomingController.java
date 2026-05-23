package com.spamascotas.spa_mascotas_api.controller.grooming;

import com.spamascotas.spa_mascotas_api.dto.request.FichaRequest;
import com.spamascotas.spa_mascotas_api.dto.request.InsumoGroomingRequest;
import com.spamascotas.spa_mascotas_api.dto.response.CitaResponse;
import com.spamascotas.spa_mascotas_api.dto.response.FichaResponse;
import com.spamascotas.spa_mascotas_api.dto.response.GroomerProductividadResponse;
import com.spamascotas.spa_mascotas_api.dto.response.HorarioTrabajoResponse;
import com.spamascotas.spa_mascotas_api.dto.response.InsumoGroomingResponse;
import com.spamascotas.spa_mascotas_api.model.Cita;
import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.repository.CitaRepository;
import com.spamascotas.spa_mascotas_api.repository.EmpleadoRepository;
import com.spamascotas.spa_mascotas_api.repository.HorarioTrabajoRepository;
import com.spamascotas.spa_mascotas_api.service.grooming.FichaGroomingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grooming")
@RequiredArgsConstructor
public class GroomingController {

    private final EmpleadoRepository empleadoRepo;
    private final HorarioTrabajoRepository horarioRepo;
    private final CitaRepository citaRepo;
    private final FichaGroomingService fichaService;

    @GetMapping("/mi-productividad")
    @PreAuthorize("hasAuthority('ROLE_GROOMER')")
    public GroomerProductividadResponse miProductividad(@AuthenticationPrincipal UserDetails user) {
        return fichaService.miProductividad(user.getUsername());
    }

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

    @GetMapping("/citas-activas")
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RECEPCION')")
    public List<CitaResponse> citasActivas(@AuthenticationPrincipal UserDetails user) {
        Empleado emp = empleadoRepo.findByUsuarioCorreo(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Perfil de empleado no encontrado"));
        return citaRepo.findByEmpleadoIdAndEstado(emp.getId(), "ACEPTADO")
                .stream().map(this::toCitaResponse).toList();
    }

    @GetMapping("/citas-activas/todas")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION') or hasAuthority('ROLE_ADMIN')")
    public List<CitaResponse> todasCitasActivas() {
        return citaRepo.findAllAceptadas()
                .stream().map(this::toCitaResponse).toList();
    }

    @GetMapping("/citas-activas/{groomerId}")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION') or hasAuthority('ROLE_ADMIN')")
    public List<CitaResponse> citasActivasPorGroomer(@PathVariable Long groomerId) {
        return citaRepo.findByEmpleadoIdAndEstado(groomerId, "ACEPTADO")
                .stream().map(this::toCitaResponse).toList();
    }

    @GetMapping("/fichas/cita/{citaId}")
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RECEPCION')")
    public ResponseEntity<FichaResponse> getFichaPorCita(@PathVariable Long citaId) {
        FichaResponse resp = fichaService.getFichaPorCita(citaId);
        return resp != null ? ResponseEntity.ok(resp) : ResponseEntity.notFound().build();
    }

    @PostMapping("/fichas")
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN')")
    public FichaResponse guardarFicha(@RequestBody FichaRequest req) {
        return fichaService.guardarFicha(req);
    }

    @PostMapping("/fichas/{id}/cerrar")
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN')")
    public FichaResponse cerrarFicha(@PathVariable Long id) {
        return fichaService.cerrarFicha(id);
    }

    @PostMapping(value = "/fichas/{citaId}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN')")
    public FichaResponse.FotoResponse subirFoto(
            @PathVariable Long citaId,
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "DURANTE") String momento) {
        return fichaService.subirFoto(citaId, file, momento);
    }

    @DeleteMapping("/fotos/{id}")
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Long id) {
        fichaService.eliminarFoto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/insumos/cita/{citaId}")
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RECEPCION')")
    public List<InsumoGroomingResponse> insumosPorCita(@PathVariable Long citaId) {
        return fichaService.listarInsumosPorCita(citaId);
    }

    @GetMapping("/insumos")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION') or hasAuthority('ROLE_ADMIN')")
    public List<InsumoGroomingResponse> todasEntregas() {
        return fichaService.listarTodasEntregas();
    }

    @PostMapping("/insumos")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION') or hasAuthority('ROLE_ADMIN')")
    public InsumoGroomingResponse entregarInsumo(@RequestBody InsumoGroomingRequest req) {
        return fichaService.entregarInsumo(req);
    }

    @PostMapping("/insumos/solicitar")
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN')")
    public InsumoGroomingResponse solicitarInsumo(@RequestBody InsumoGroomingRequest req,
                                                   @AuthenticationPrincipal UserDetails user) {
        Empleado emp = empleadoRepo.findByUsuarioCorreo(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Perfil de groomer no encontrado"));
        req.setEmpleadoId(emp.getId());
        return fichaService.solicitarInsumo(req);
    }

    @PutMapping("/insumos/{id}/estado")
    @PreAuthorize("hasAuthority('ROLE_GROOMER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RECEPCION')")
    public InsumoGroomingResponse actualizarEstadoInsumo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return fichaService.actualizarEstadoInsumo(id, body.get("estado"));
    }

    private CitaResponse toCitaResponse(Cita c) {
        return CitaResponse.builder()
                .id(c.getId())
                .mascotaNombre(c.getMascota() != null ? c.getMascota().getNombre() : null)
                .clienteNombre(c.getCliente() != null ? c.getCliente().getNombre() : null)
                .servicioNombre(c.getServicio() != null ? c.getServicio().getNombre() : null)
                .empleadoAsignadoId(c.getEmpleadoAsignado() != null ? c.getEmpleadoAsignado().getId() : null)
                .empleadoAsignadoNombre(c.getEmpleadoAsignado() != null ? c.getEmpleadoAsignado().getNombre() : null)
                .fechaHoraInicio(c.getFechaHoraInicio())
                .estado(c.getEstado())
                .build();
    }
}
