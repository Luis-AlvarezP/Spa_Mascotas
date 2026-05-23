package com.spamascotas.spa_mascotas_api.service.historial;

import com.spamascotas.spa_mascotas_api.dto.request.CalificacionRequest;
import com.spamascotas.spa_mascotas_api.dto.response.CalificacionResponse;
import com.spamascotas.spa_mascotas_api.dto.response.FichaResponse;
import com.spamascotas.spa_mascotas_api.dto.response.HistorialCitaResponse;
import com.spamascotas.spa_mascotas_api.model.*;
import com.spamascotas.spa_mascotas_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistorialService {

    private final CitaRepository           citaRepo;
    private final ClienteRepository        clienteRepo;
    private final EmpleadoRepository       empleadoRepo;
    private final FichaIngresoRepository   fichaRepo;
    private final FichaDetalleRepository   detalleRepo;
    private final GaleriaMascotaRepository galeriaRepo;
    private final RecomendacionRepository  recomendacionRepo;
    private final CalificacionCitaRepository califRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public List<HistorialCitaResponse> historialPorMascota(Long mascotaId, String correoCliente) {
        Cliente cliente = clienteRepo.findByUsuarioCorreo(correoCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return citaRepo.findByMascotaIdAndEstadoOrderByFechaHoraInicioDesc(mascotaId, "REALIZADO")
                .stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getId().equals(cliente.getId()))
                .map(this::toHistorial)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistorialCitaResponse> historialPorMascotaStaff(Long mascotaId) {
        return citaRepo.findByMascotaIdAndEstadoOrderByFechaHoraInicioDesc(mascotaId, "REALIZADO")
                .stream().map(this::toHistorial).toList();
    }

    @Transactional(readOnly = true)
    public List<HistorialCitaResponse> historialGroomer(String correoGroomer) {
        Empleado groomer = empleadoRepo.findByUsuarioCorreo(correoGroomer)
                .orElseThrow(() -> new RuntimeException("Groomer no encontrado"));
        return citaRepo.findRealizadasByGroomer(groomer.getId())
                .stream().map(this::toHistorial).toList();
    }

    @Transactional
    public CalificacionResponse calificar(CalificacionRequest req, String correoCliente) {
        Cita cita = citaRepo.findById(req.getCitaId())
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        if (!"REALIZADO".equals(cita.getEstado()))
            throw new RuntimeException("Solo se pueden calificar citas realizadas");
        if (califRepo.findByCitaId(req.getCitaId()).isPresent())
            throw new RuntimeException("Esta cita ya fue calificada");
        CalificacionCita calif = CalificacionCita.builder()
                .cita(cita).puntuacion(req.getPuntuacion())
                .comentario(req.getComentario()).fecha(LocalDateTime.now()).build();
        return toCalifResponse(califRepo.save(calif));
    }

    private HistorialCitaResponse toHistorial(Cita cita) {
        FichaIngreso ficha = fichaRepo.findByCitaId(cita.getId()).orElse(null);
        List<String> detalles = ficha != null
                ? detalleRepo.findByFichaId(ficha.getId()).stream().map(FichaDetalle::getDescripcionEstado).toList()
                : List.of();
        List<FichaResponse.FotoResponse> fotos = galeriaRepo.findByCitaIdOrderByFechaCarga(cita.getId())
                .stream().map(g -> FichaResponse.FotoResponse.builder()
                        .id(g.getId()).url(g.getUrlImage()).momento(g.getMomento()).build())
                .toList();
        RecomendacionPostServicio rec = recomendacionRepo.findByCitaId(cita.getId()).orElse(null);
        CalificacionCita calif = califRepo.findByCitaId(cita.getId()).orElse(null);
        Mascota mascota = cita.getMascota();

        Cliente cli = cita.getCliente();
        return HistorialCitaResponse.builder()
                .citaId(cita.getId())
                .clienteNombre(cli != null ? cli.getNombre() : null)
                .clienteCi(cli != null ? cli.getCi() : null)
                .mascotaNombre(mascota != null ? mascota.getNombre() : null)
                .mascotaEspecie(mascota != null ? mascota.getEspecie() : null)
                .mascotaTamano(mascota != null ? mascota.getTamano() : null)
                .mascotaFotoUrl(mascota != null ? mascota.getUrlFotoMascota() : null)
                .servicio(cita.getServicio() != null ? cita.getServicio().getNombre() : null)
                .groomer(cita.getEmpleadoAsignado() != null ? cita.getEmpleadoAsignado().getNombre() : null)
                .fechaHoraInicio(cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(FMT) : null)
                .fechaHoraFin(cita.getFechaHoraFin() != null ? cita.getFechaHoraFin().format(FMT) : null)
                .precioFinal(cita.getPrecioFinal() != null ? cita.getPrecioFinal().doubleValue() : null)
                .metodoPago(cita.getMetodoPago())
                .estadoIngreso(ficha != null ? ficha.getEstadoIngreso() : null)
                .temperamento(cita.getTemperamentoMascota())
                .notasCita(cita.getNotas())
                .observaciones(ficha != null ? ficha.getObservacionesGenerales() : null)
                .checklistBano(ficha != null ? ficha.getChecklistBano() : null)
                .checklistCorte(ficha != null ? ficha.getChecklistCorte() : null)
                .checklistUnas(ficha != null ? ficha.getChecklistUnas() : null)
                .checklistOidos(ficha != null ? ficha.getChecklistOidos() : null)
                .checklistGlandulas(ficha != null ? ficha.getChecklistGlandulas() : null)
                .checklistPerfume(ficha != null ? ficha.getChecklistPerfume() : null)
                .checklistPeinado(ficha != null ? ficha.getChecklistPeinado() : null)
                .detalles(detalles)
                .fotos(fotos)
                .recomendacion(rec != null ? rec.getRecomendacion() : null)
                .proximaCitaSugerida(rec != null && rec.getProximaCitaSugerida() != null
                        ? rec.getProximaCitaSugerida().toString() : null)
                .calificacion(calif != null ? toCalifResponse(calif) : null)
                .build();
    }

    private CalificacionResponse toCalifResponse(CalificacionCita c) {
        return CalificacionResponse.builder()
                .id(c.getId()).citaId(c.getCita().getId())
                .puntuacion(c.getPuntuacion()).comentario(c.getComentario()).fecha(c.getFecha())
                .build();
    }
}
