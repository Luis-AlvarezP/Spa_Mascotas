package com.spamascotas.spa_mascotas_api.service.citas;

import com.spamascotas.spa_mascotas_api.dto.request.CancelacionRequest;
import com.spamascotas.spa_mascotas_api.dto.request.CitaRequest;
import com.spamascotas.spa_mascotas_api.dto.request.CobrarRequest;
import com.spamascotas.spa_mascotas_api.dto.request.RechazarRequest;
import com.spamascotas.spa_mascotas_api.dto.request.ReprogramarRequest;
import com.spamascotas.spa_mascotas_api.service.auth.EmailService;
import com.spamascotas.spa_mascotas_api.dto.response.CitaResponse;
import com.spamascotas.spa_mascotas_api.dto.response.GroomerResponse;
import com.spamascotas.spa_mascotas_api.dto.response.ServicioResponse;
import com.spamascotas.spa_mascotas_api.dto.response.SlotResponse;
import com.spamascotas.spa_mascotas_api.model.*;
import com.spamascotas.spa_mascotas_api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitaService {

    private static final Map<DayOfWeek, String> DIA_ES = Map.of(
            DayOfWeek.MONDAY,    "LUNES",
            DayOfWeek.TUESDAY,   "MARTES",
            DayOfWeek.WEDNESDAY, "MIERCOLES",
            DayOfWeek.THURSDAY,  "JUEVES",
            DayOfWeek.FRIDAY,    "VIERNES",
            DayOfWeek.SATURDAY,  "SABADO",
            DayOfWeek.SUNDAY,    "DOMINGO"
    );

    private final CitaRepository            citaRepo;
    private final ClienteRepository         clienteRepo;
    private final MascotaRepository         mascotaRepo;
    private final ServicioRepository        servicioRepo;
    private final EmpleadoRepository        empleadoRepo;
    private final HorarioTrabajoRepository  horarioRepo;
    private final BloqueoAgendaRepository   bloqueoRepo;
    private final EmailService              emailService;

    // ── Servicios disponibles ────────────────────────────────

    @Transactional(readOnly = true)
    public List<ServicioResponse> listarServicios() {
        return servicioRepo.findByActivoTrue().stream().map(s ->
            ServicioResponse.builder()
                .id(s.getId()).nombre(s.getNombre()).descripcion(s.getDescripcion())
                .duracionMinutos(s.getDuracionMinutos()).precioBase(s.getPrecioBase())
                .activo(s.getActivo()).build()
        ).toList();
    }

    // ── Groomers disponibles (para clientes) ─────────────────

    @Transactional(readOnly = true)
    public List<GroomerResponse> listarGroomers() {
        return empleadoRepo.findByPuesto("GROOMER").stream()
                .filter(e -> e.getUsuario() != null && e.getUsuario().getEstado() != null
                          && "ACTIVO".equals(e.getUsuario().getEstado().name()))
                .map(e -> GroomerResponse.builder()
                        .id(e.getId())
                        .nombre(e.getNombre())
                        .correo(e.getUsuario().getCorreo())
                        .activo(true)
                        .build())
                .toList();
    }

    // ── Slots disponibles ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SlotResponse> slotsDisponibles(LocalDate fecha, Long servicioId, Long mascotaId,
                                               Long empleadoId, Long excluirCitaId) {
        Servicio servicio = servicioRepo.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        Mascota mascota = mascotaRepo.findById(mascotaId)
                .orElseThrow(() -> new RuntimeException("Mascota no encontrada"));

        int duracion = calcularDuracion(servicio, mascota);
        String diaSemana = DIA_ES.get(fecha.getDayOfWeek());

        if (empleadoId != null) {
            return slotsParaGroomer(fecha, duracion, diaSemana, empleadoId, excluirCitaId);
        }

        // Modo multi-groomer: muestra disponibilidad de todos los groomers activos
        List<Empleado> groomers = empleadoRepo.findByPuesto("GROOMER").stream()
                .filter(e -> e.getUsuario() != null && e.getUsuario().getEstado() != null
                          && "ACTIVO".equals(e.getUsuario().getEstado().name()))
                .toList();

        LocalDateTime diaCompleto  = fecha.atStartOfDay();
        LocalDateTime diaSiguiente = fecha.plusDays(1).atStartOfDay();

        Map<Long, List<HorarioTrabajo>> horarioMap = new HashMap<>();
        Map<Long, List<Cita>> citaMap = new HashMap<>();
        for (Empleado g : groomers) {
            horarioMap.put(g.getId(), horarioRepo.findVigenteByEmpleadoAndDia(g.getId(), diaSemana, fecha));
            List<Cita> aceptadas = citaRepo.findAceptadasEnDiaByEmpleado(diaCompleto, diaSiguiente, g.getId())
                    .stream().filter(c -> excluirCitaId == null || !c.getId().equals(excluirCitaId)).toList();
            citaMap.put(g.getId(), aceptadas);
        }

        // Sala: todas las citas activas del día excluyendo la cita en reprogramación
        List<Cita>          todasCitasDelDia = citaRepo.findActivasEnDia(diaCompleto, diaSiguiente)
                .stream().filter(c -> excluirCitaId == null || !c.getId().equals(excluirCitaId)).toList();
        List<BloqueoAgenda> bloqueosDelDia   = bloqueoRepo.findActivasEnRango(diaCompleto, diaSiguiente);
        List<BloqueoAgenda> bloqueosSpa      = bloqueosDelDia.stream()
                .filter(b -> b.getEmpleado() == null).toList();
        Map<Long, List<BloqueoAgenda>> bloqueosGroomer = new HashMap<>();
        for (BloqueoAgenda b : bloqueosDelDia) {
            if (b.getEmpleado() != null) {
                bloqueosGroomer.computeIfAbsent(b.getEmpleado().getId(), k -> new ArrayList<>()).add(b);
            }
        }

        List<SlotResponse> result = new ArrayList<>();
        LocalDateTime cursor = fecha.atTime(LocalTime.of(8, 0));

        LocalDateTime limiteAnticipacion = LocalDateTime.now().plusHours(3);
        while (!cursor.plusMinutes(duracion).isAfter(fecha.atTime(LocalTime.of(18, 0)))) {
            final LocalDateTime slotInicio = cursor;
            final LocalDateTime slotFin    = cursor.plusMinutes(duracion);

            if (fecha.isEqual(LocalDate.now()) && !slotInicio.isAfter(limiteAnticipacion)) {
                cursor = cursor.plusMinutes(30);
                continue;
            }

            // Sala ocupada por cita existente
            boolean salaOcupada = todasCitasDelDia.stream().anyMatch(c ->
                c.getFechaHoraInicio().isBefore(slotFin) && c.getFechaHoraFin().isAfter(slotInicio)
            );
            // Bloqueo de todo el spa en este slot
            boolean spaBloqueado = bloqueosSpa.stream().anyMatch(b ->
                b.getFechaInicio().isBefore(slotFin) && b.getFechaFin().isAfter(slotInicio)
            );

            if (!salaOcupada && !spaBloqueado) {
                List<SlotResponse.GroomerInfo> disponibles = new ArrayList<>();
                for (Empleado g : groomers) {
                    List<HorarioTrabajo> horarios = horarioMap.get(g.getId());
                    boolean trabaja = horarios.stream().anyMatch(h ->
                        !fecha.atTime(h.getHoraInicio()).isAfter(slotInicio) &&
                        !fecha.atTime(h.getHoraFin()).isBefore(slotFin)
                    );
                    if (!trabaja) continue;

                    // Bloqueo personal del groomer
                    boolean groomerBloqueado = bloqueosGroomer.getOrDefault(g.getId(), List.of())
                            .stream().anyMatch(b ->
                                b.getFechaInicio().isBefore(slotFin) && b.getFechaFin().isAfter(slotInicio));
                    if (groomerBloqueado) continue;

                    // Capacidad máxima del groomer para ese día
                    int capacidad = horarios.get(0).getCapacidadMaxima();
                    if (citaMap.get(g.getId()).size() >= capacidad) continue;

                    disponibles.add(SlotResponse.GroomerInfo.builder()
                        .id(g.getId()).nombre(g.getNombre()).build());
                }

                if (!disponibles.isEmpty()) {
                    result.add(SlotResponse.builder()
                        .inicio(slotInicio).fin(slotFin).disponible(true).groomers(disponibles).build());
                }
            }
            cursor = cursor.plusMinutes(30);
        }
        return result;
    }

    private List<SlotResponse> slotsParaGroomer(LocalDate fecha, int duracion, String diaSemana,
                                               Long empleadoId, Long excluirCitaId) {
        List<HorarioTrabajo> horarios = horarioRepo.findVigenteByEmpleadoAndDia(empleadoId, diaSemana, fecha);
        if (horarios.isEmpty()) return List.of();
        Empleado emp = empleadoRepo.findById(empleadoId).orElse(null);
        if (emp == null) return List.of();

        LocalDateTime diaInicio   = fecha.atTime(horarios.get(0).getHoraInicio());
        LocalDateTime diaFin      = fecha.atTime(horarios.get(0).getHoraFin());
        LocalDateTime diaCompleto = fecha.atStartOfDay();
        LocalDateTime diaSig      = fecha.plusDays(1).atStartOfDay();

        List<Cita>          todasCitasDelDia  = citaRepo.findActivasEnDia(diaCompleto, diaSig)
                .stream().filter(c -> excluirCitaId == null || !c.getId().equals(excluirCitaId)).toList();
        List<Cita>          citasGroomer      = citaRepo.findAceptadasEnDiaByEmpleado(diaCompleto, diaSig, empleadoId)
                .stream().filter(c -> excluirCitaId == null || !c.getId().equals(excluirCitaId)).toList();
        List<BloqueoAgenda> bloqueosDelDia    = bloqueoRepo.findActivasEnRango(diaCompleto, diaSig);
        List<BloqueoAgenda> bloqueosSpa       = bloqueosDelDia.stream().filter(b -> b.getEmpleado() == null).toList();
        List<BloqueoAgenda> bloqueosEsteGroomer = bloqueosDelDia.stream()
                .filter(b -> b.getEmpleado() != null && b.getEmpleado().getId().equals(empleadoId)).toList();

        int capacidadMaxima   = horarios.get(0).getCapacidadMaxima();
        boolean capacidadExcedida = citasGroomer.size() >= capacidadMaxima;

        LocalDateTime limiteAnticipacion = LocalDateTime.now().plusHours(3);
        List<SlotResponse> slots = new ArrayList<>();
        LocalDateTime cursor = diaInicio;
        while (!cursor.plusMinutes(duracion).isAfter(diaFin)) {
            final LocalDateTime slotInicio = cursor;
            final LocalDateTime slotFin    = cursor.plusMinutes(duracion);

            if (fecha.isEqual(LocalDate.now()) && !slotInicio.isAfter(limiteAnticipacion)) {
                cursor = cursor.plusMinutes(30);
                continue;
            }

            boolean salaOcupada = todasCitasDelDia.stream().anyMatch(c ->
                c.getFechaHoraInicio().isBefore(slotFin) && c.getFechaHoraFin().isAfter(slotInicio)
            );
            boolean bloqueado = bloqueosSpa.stream().anyMatch(b ->
                    b.getFechaInicio().isBefore(slotFin) && b.getFechaFin().isAfter(slotInicio))
                || bloqueosEsteGroomer.stream().anyMatch(b ->
                    b.getFechaInicio().isBefore(slotFin) && b.getFechaFin().isAfter(slotInicio));

            boolean disponible = !salaOcupada && !bloqueado && !capacidadExcedida;

            List<SlotResponse.GroomerInfo> gi = disponible
                ? List.of(SlotResponse.GroomerInfo.builder().id(emp.getId()).nombre(emp.getNombre()).build())
                : List.of();
            slots.add(SlotResponse.builder()
                .inicio(slotInicio).fin(slotFin).disponible(disponible).groomers(gi).build());
            cursor = cursor.plusMinutes(30);
        }
        return slots;
    }

    // ── Solicitar cita ───────────────────────────────────────

    @Transactional
    public CitaResponse solicitar(CitaRequest req, String correoCliente) {
        Cliente cliente = clienteRepo.findByUsuarioCorreo(correoCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        Mascota mascota = mascotaRepo.findById(req.getMascotaId())
                .orElseThrow(() -> new RuntimeException("Mascota no encontrada"));
        if (!mascota.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("La mascota no pertenece a este cliente");
        }
        Servicio servicio = servicioRepo.findById(req.getServicioId())
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        Empleado preferido = req.getEmpleadoPreferidoId() != null
                ? empleadoRepo.findById(req.getEmpleadoPreferidoId()).orElse(null) : null;

        LocalDateTime inicio = LocalDateTime.parse(req.getFechaHoraInicio());
        int duracion = calcularDuracion(servicio, mascota);
        LocalDateTime fin = inicio.plusMinutes(duracion);

        validarBloqueos(inicio, fin, preferido != null ? preferido.getId() : null);
        validarSolapamiento(inicio, fin, null);
        if (preferido != null) {
            validarCapacidadGroomer(inicio.toLocalDate(), preferido, null);
        }

        BigDecimal recargo = cliente.getPenalizacionPorcentaje() != null &&
                             cliente.getPenalizacionPorcentaje().compareTo(BigDecimal.ZERO) > 0
                             ? cliente.getPenalizacionPorcentaje() : BigDecimal.ZERO;
        if (recargo.compareTo(BigDecimal.ZERO) > 0) {
            cliente.setPenalizacionPorcentaje(BigDecimal.ZERO);
            clienteRepo.save(cliente);
        }

        Cita cita = Cita.builder()
                .cliente(cliente).mascota(mascota).servicio(servicio)
                .empleadoPreferido(preferido)
                .fechaHoraInicio(inicio).fechaHoraFin(fin)
                .estado("EN_REVISION")
                .duracionMinutos(duracion)
                .tamanoMascota(mascota.getTamano())
                .temperamentoMascota(mascota.getTemperamento() != null ? mascota.getTemperamento().getNombre() : null)
                .precioFinal(calcularPrecio(servicio, mascota))
                .recargoPorcentaje(recargo)
                .notas(req.getNotas())
                .build();

        CitaResponse resp = toResponse(citaRepo.save(cita));
        try {
            String correo = cliente.getUsuario().getCorreo();
            emailService.enviarCitaEnRevision(correo, cliente.getNombre(),
                    servicio.getNombre(), fmtDT(inicio));
        } catch (Exception e) { log.warn("Email solicitar: {}", e.getMessage()); }
        return resp;
    }

    // ── Mis citas ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CitaResponse> misCitas(String correoCliente) {
        Cliente cliente = clienteRepo.findByUsuarioCorreo(correoCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return citaRepo.findByClienteIdOrderByFechaHoraInicioDesc(cliente.getId())
                .stream().map(this::toResponse).toList();
    }

    // ── Cancelar ─────────────────────────────────────────────

    @Transactional
    public CitaResponse cancelar(Long id, CancelacionRequest req, String correoCliente) {
        Cita cita = getCita(id);
        validarPropietario(cita, correoCliente);

        if (!"EN_REVISION".equals(cita.getEstado()) && !"ACEPTADO".equals(cita.getEstado())) {
            throw new RuntimeException("Solo puedes cancelar citas en estado EN_REVISION o ACEPTADO");
        }

        long horasRestantes = ChronoUnit.HOURS.between(LocalDateTime.now(), cita.getFechaHoraInicio());
        Cliente cliente = cita.getCliente();
        boolean clienteModificado = false;

        if (horasRestantes < 24) {
            BigDecimal actual = cliente.getPenalizacionPorcentaje() == null
                    ? BigDecimal.ZERO : cliente.getPenalizacionPorcentaje();
            cliente.setPenalizacionPorcentaje(actual.add(new BigDecimal("5")));
            clienteModificado = true;
            log.info("Penalización +5% aplicada al cliente {} por cancelación tardía", cliente.getId());
        }

        BigDecimal recargoCita = cita.getRecargoPorcentaje() != null
                ? cita.getRecargoPorcentaje() : BigDecimal.ZERO;
        if (recargoCita.compareTo(BigDecimal.ZERO) > 0) {
            List<Cita> proximas = citaRepo.findProximaActivaByCliente(cliente.getId(), id);
            if (!proximas.isEmpty()) {
                Cita proxima = proximas.get(0);
                proxima.setRecargoPorcentaje(recargoCita);
                citaRepo.save(proxima);
            } else {
                BigDecimal actualPen = cliente.getPenalizacionPorcentaje() == null
                        ? BigDecimal.ZERO : cliente.getPenalizacionPorcentaje();
                cliente.setPenalizacionPorcentaje(actualPen.add(recargoCita));
                clienteModificado = true;
            }
        }

        if (clienteModificado) {
            clienteRepo.save(cliente);
        }

        cita.setEstado("CANCELADO");
        cita.setMotivoCancelacion(req.getMotivo());
        CitaResponse resp = toResponse(citaRepo.save(cita));
        try {
            emailService.enviarCitaCancelada(cliente.getUsuario().getCorreo(),
                    cliente.getNombre(),
                    cita.getServicio().getNombre(),
                    fmtDT(cita.getFechaHoraInicio()),
                    req.getMotivo());
        } catch (Exception e) { log.warn("Email cancelar: {}", e.getMessage()); }
        return resp;
    }

    // ── Reprogramar ──────────────────────────────────────────

    @Transactional
    public CitaResponse reprogramar(Long id, ReprogramarRequest req, String correoCliente) {
        Cita original = getCita(id);
        validarPropietario(original, correoCliente);

        if (!"EN_REVISION".equals(original.getEstado()) && !"ACEPTADO".equals(original.getEstado())) {
            throw new RuntimeException("Solo puedes reprogramar citas en estado EN_REVISION o ACEPTADO");
        }

        LocalDateTime nuevoInicio = LocalDateTime.parse(req.getFechaHoraInicio());
        int duracion = calcularDuracion(original.getServicio(), original.getMascota());
        LocalDateTime nuevoFin = nuevoInicio.plusMinutes(duracion);

        Empleado nuevoPreferido = req.getEmpleadoPreferidoId() != null
                ? empleadoRepo.findById(req.getEmpleadoPreferidoId()).orElse(original.getEmpleadoPreferido())
                : original.getEmpleadoPreferido();

        // Validar antes de cancelar la cita original (id excluido del solapamiento y capacidad)
        validarBloqueos(nuevoInicio, nuevoFin, nuevoPreferido != null ? nuevoPreferido.getId() : null);
        validarSolapamiento(nuevoInicio, nuevoFin, id);
        if (nuevoPreferido != null) {
            validarCapacidadGroomer(nuevoInicio.toLocalDate(), nuevoPreferido, id);
        }

        original.setEstado("CANCELADO");
        original.setMotivoCancelacion("Reprogramada por el cliente");
        citaRepo.save(original);

        Mascota mascotaNueva = original.getMascota();
        Cita nueva = Cita.builder()
                .cliente(original.getCliente())
                .mascota(mascotaNueva)
                .servicio(original.getServicio())
                .empleadoPreferido(nuevoPreferido)
                .fechaHoraInicio(nuevoInicio).fechaHoraFin(nuevoFin)
                .estado("EN_REVISION")
                .duracionMinutos(duracion)
                .tamanoMascota(mascotaNueva.getTamano())
                .temperamentoMascota(mascotaNueva.getTemperamento() != null ? mascotaNueva.getTemperamento().getNombre() : null)
                .precioFinal(original.getPrecioFinal())
                .notas(req.getNotas())
                .build();

        CitaResponse resp = toResponse(citaRepo.save(nueva));
        try {
            Cliente cl = original.getCliente();
            emailService.enviarCitaReprogramada(cl.getUsuario().getCorreo(),
                    cl.getNombre(), original.getServicio().getNombre(), fmtDT(nuevoInicio));
        } catch (Exception e) { log.warn("Email reprogramar: {}", e.getMessage()); }
        return resp;
    }

    // ── Aceptar (Recepcion/Admin) ────────────────────────────

    @Transactional
    public CitaResponse aceptar(Long id) {
        Cita cita = getCita(id);
        if (!"EN_REVISION".equals(cita.getEstado())) {
            throw new RuntimeException("Solo se pueden aceptar citas en estado EN_REVISION");
        }
        if (cita.getEmpleadoPreferido() != null && cita.getEmpleadoAsignado() == null) {
            cita.setEmpleadoAsignado(cita.getEmpleadoPreferido());
        }
        cita.setEstado("ACEPTADO");
        CitaResponse resp = toResponse(citaRepo.save(cita));
        try {
            Cliente cl = cita.getCliente();
            Empleado groomer = cita.getEmpleadoAsignado() != null
                    ? cita.getEmpleadoAsignado() : cita.getEmpleadoPreferido();
            emailService.enviarCitaConfirmada(cl.getUsuario().getCorreo(),
                    cl.getNombre(), cita.getServicio().getNombre(),
                    fmtDT(cita.getFechaHoraInicio()),
                    groomer != null ? groomer.getNombre() : null);
        } catch (Exception e) { log.warn("Email aceptar: {}", e.getMessage()); }
        return resp;
    }

    // ── Rechazar (Recepcion/Admin) ───────────────────────────

    @Transactional
    public CitaResponse rechazar(Long id, RechazarRequest req) {
        Cita cita = getCita(id);
        if (!"EN_REVISION".equals(cita.getEstado())) {
            throw new RuntimeException("Solo se pueden rechazar citas en estado EN_REVISION");
        }
        cita.setEstado("CANCELADO");
        cita.setMotivoCancelacion(req.getMotivo());
        CitaResponse resp = toResponse(citaRepo.save(cita));
        try {
            Cliente cl = cita.getCliente();
            emailService.enviarCitaCancelada(cl.getUsuario().getCorreo(),
                    cl.getNombre(), cita.getServicio().getNombre(),
                    fmtDT(cita.getFechaHoraInicio()), req.getMotivo());
        } catch (Exception e) { log.warn("Email rechazar: {}", e.getMessage()); }
        return resp;
    }

    // ── Finalizar servicio (Groomer) ─────────────────────────

    @Transactional
    public CitaResponse finalizarServicio(Long id) {
        Cita cita = getCita(id);
        if (!"ACEPTADO".equals(cita.getEstado())) {
            throw new RuntimeException("Solo se pueden finalizar citas en estado ACEPTADO");
        }
        cita.setEstado("PENDIENTE_PAGO");
        CitaResponse resp = toResponse(citaRepo.save(cita));
        try {
            if (cita.getCliente() != null) {
                String correo  = cita.getCliente().getUsuario().getCorreo();
                String cliente = cita.getCliente().getNombre();
                String mascota = cita.getMascota() != null ? cita.getMascota().getNombre() : "tu mascota";
                String groomer = cita.getEmpleadoAsignado() != null
                        ? cita.getEmpleadoAsignado().getNombre() : "nuestro equipo";
                emailService.enviarListaParaRecoger(correo, cliente, mascota,
                        cita.getServicio().getNombre(), groomer);
            }
        } catch (Exception ignored) {}
        return resp;
    }

    // ── Cobrar (Recepcion/Admin) ─────────────────────────────

    @Transactional
    public CitaResponse cobrar(Long id, CobrarRequest req) {
        Cita cita = getCita(id);
        if (!"PENDIENTE_PAGO".equals(cita.getEstado())) {
            throw new RuntimeException("Solo se pueden cobrar citas en estado PENDIENTE_PAGO");
        }
        cita.setEstado("REALIZADO");
        cita.setMetodoPago(req.getMetodoPago());
        CitaResponse resp = toResponse(citaRepo.save(cita));
        try {
            Cliente cl = cita.getCliente();
            BigDecimal recargo = cita.getRecargoPorcentaje() != null ? cita.getRecargoPorcentaje() : BigDecimal.ZERO;
            BigDecimal base    = cita.getPrecioFinal();
            BigDecimal total   = recargo.compareTo(BigDecimal.ZERO) > 0
                    ? base.add(base.multiply(recargo).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP))
                    : base;
            String groomer   = cita.getEmpleadoAsignado() != null ? cita.getEmpleadoAsignado().getNombre() : "Por asignar";
            String duracion  = cita.getDuracionMinutos() != null ? cita.getDuracionMinutos().toString() : "—";
            String mascota   = cita.getMascota() != null ? cita.getMascota().getNombre() : "—";
            emailService.enviarReciboCita(
                    cl.getUsuario().getCorreo(),
                    cl.getNombre(),
                    cl.getCi(),
                    mascota,
                    cita.getServicio().getNombre(),
                    groomer,
                    duracion,
                    fmtDT(cita.getFechaHoraInicio()),
                    req.getMetodoPago(),
                    base.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    recargo.stripTrailingZeros().toPlainString(),
                    total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    cita.getId());
        } catch (Exception e) { log.warn("Email cobrar: {}", e.getMessage()); }
        return resp;
    }

    // ── Todas las citas (Staff) ──────────────────────────────

    @Transactional(readOnly = true)
    public List<CitaResponse> todasLasCitas() {
        return citaRepo.findAllActivasOrderByFecha().stream().map(this::toResponse).toList();
    }

    // ── Mis servicios (Groomer) ──────────────────────────────

    @Transactional(readOnly = true)
    public List<CitaResponse> misServicios(String correoGroomer) {
        Empleado groomer = empleadoRepo.findByUsuarioCorreo(correoGroomer)
                .orElseThrow(() -> new RuntimeException("Groomer no encontrado"));
        return citaRepo.findServiciosPendientesByEmpleado(groomer.getId())
                .stream().map(this::toResponse).toList();
    }

    // ── Cálculo duración y precio ────────────────────────────

    public int calcularDuracion(Servicio servicio, Mascota mascota) {
        int dur = servicio.getDuracionMinutos();
        String tamano = mascota.getTamano() != null ? mascota.getTamano().toUpperCase().trim() : "";
        String temp   = mascota.getTemperamento() != null
                ? mascota.getTemperamento().getNombre().toUpperCase().trim() : "TRANQUILO";

        dur += switch (tamano) {
            case "MEDIANO", "MEDIANA" -> 5;
            case "GRANDE"             -> 10;
            case "GIGANTE"            -> 15;
            default                   -> 0;
        };
        dur += switch (temp) {
            case "NERVIOSO", "INQUIETO" -> 5;
            case "AGRESIVO"             -> 10;
            default                     -> 0;
        };
        return dur;
    }

    public BigDecimal calcularPrecio(Servicio servicio, Mascota mascota) {
        BigDecimal precio = servicio.getPrecioBase();
        String tamano = mascota.getTamano() != null ? mascota.getTamano().toUpperCase().trim() : "";
        String temp   = mascota.getTemperamento() != null
                ? mascota.getTemperamento().getNombre().toUpperCase().trim() : "TRANQUILO";

        precio = precio.add(switch (tamano) {
            case "MEDIANO", "MEDIANA" -> new BigDecimal("10");
            case "GRANDE"             -> new BigDecimal("15");
            case "GIGANTE"            -> new BigDecimal("20");
            default                   -> BigDecimal.ZERO;
        });
        precio = precio.add(switch (temp) {
            case "NERVIOSO", "INQUIETO" -> new BigDecimal("5");
            case "AGRESIVO"             -> new BigDecimal("10");
            default                     -> BigDecimal.ZERO;
        });
        return precio;
    }

    // ── Helpers ──────────────────────────────────────────────

    private void validarSolapamiento(LocalDateTime inicio, LocalDateTime fin, Long excluirId) {
        List<Cita> solapadas = citaRepo.findSolapadas(inicio, fin);
        if (excluirId != null) {
            solapadas = solapadas.stream().filter(c -> !c.getId().equals(excluirId)).toList();
        }
        if (!solapadas.isEmpty()) {
            throw new RuntimeException("El horario seleccionado ya está ocupado. Por favor elige otro.");
        }
    }

    private void validarBloqueos(LocalDateTime inicio, LocalDateTime fin, Long empleadoId) {
        List<BloqueoAgenda> bloqueos = bloqueoRepo.findActivasEnRango(inicio, fin);
        boolean spaBloqueo = bloqueos.stream().anyMatch(b -> b.getEmpleado() == null);
        if (spaBloqueo) {
            throw new RuntimeException("No se pueden reservar citas en ese período (el spa está bloqueado).");
        }
        if (empleadoId != null) {
            boolean groomerBloqueo = bloqueos.stream()
                    .anyMatch(b -> b.getEmpleado() != null && b.getEmpleado().getId().equals(empleadoId));
            if (groomerBloqueo) {
                throw new RuntimeException("El groomer seleccionado no está disponible en ese horario.");
            }
        }
    }

    private void validarCapacidadGroomer(LocalDate fecha, Empleado groomer, Long excluirCitaId) {
        String diaSemana = DIA_ES.get(fecha.getDayOfWeek());
        List<HorarioTrabajo> horarios = horarioRepo.findVigenteByEmpleadoAndDia(groomer.getId(), diaSemana, fecha);
        if (horarios.isEmpty()) return;
        int capacidadMaxima = horarios.get(0).getCapacidadMaxima();
        long citasHoy = citaRepo.findAceptadasEnDiaByEmpleado(
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay(), groomer.getId()
        ).stream().filter(c -> excluirCitaId == null || !c.getId().equals(excluirCitaId)).count();
        if (citasHoy >= capacidadMaxima) {
            throw new RuntimeException(groomer.getNombre()
                + " ya alcanzó su capacidad máxima de " + capacidadMaxima + " servicios para ese día.");
        }
    }

    private static final DateTimeFormatter FMT_DT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String fmtDT(LocalDateTime dt) {
        return dt != null ? dt.format(FMT_DT) : "—";
    }

    private void validarPropietario(Cita cita, String correoCliente) {
        if (!cita.getCliente().getUsuario().getCorreo().equals(correoCliente)) {
            throw new RuntimeException("No autorizado");
        }
    }

    private Cita getCita(Long id) {
        return citaRepo.findById(id).orElseThrow(() -> new RuntimeException("Cita no encontrada"));
    }

    private CitaResponse toResponse(Cita c) {
        return CitaResponse.builder()
                .id(c.getId())
                .estado(c.getEstado())
                .clienteId(c.getCliente() != null ? c.getCliente().getId() : null)
                .clienteNombre(c.getCliente() != null ? c.getCliente().getNombre() : null)
                .clienteCorreo(c.getCliente() != null && c.getCliente().getUsuario() != null
                        ? c.getCliente().getUsuario().getCorreo() : null)
                .clienteCi(c.getCliente() != null ? c.getCliente().getCi() : null)
                .mascotaId(c.getMascota() != null ? c.getMascota().getId() : null)
                .mascotaNombre(c.getMascota() != null ? c.getMascota().getNombre() : null)
                .mascotaEspecie(c.getMascota() != null ? c.getMascota().getEspecie() : null)
                .mascotaTamano(c.getTamanoMascota() != null ? c.getTamanoMascota()
                        : (c.getMascota() != null ? c.getMascota().getTamano() : null))
                .mascotaTemperamento(c.getTemperamentoMascota() != null ? c.getTemperamentoMascota()
                        : (c.getMascota() != null && c.getMascota().getTemperamento() != null
                                ? c.getMascota().getTemperamento().getNombre() : null))
                .mascotaTemperamentoColor(c.getMascota() != null && c.getMascota().getTemperamento() != null
                        ? c.getMascota().getTemperamento().getColorAlerta() : null)
                .servicioId(c.getServicio() != null ? c.getServicio().getId() : null)
                .servicioNombre(c.getServicio() != null ? c.getServicio().getNombre() : null)
                .duracionMinutos(c.getDuracionMinutos() != null ? c.getDuracionMinutos()
                        : (c.getServicio() != null && c.getMascota() != null
                                ? calcularDuracion(c.getServicio(), c.getMascota()) : null))
                .empleadoAsignadoId(c.getEmpleadoAsignado() != null ? c.getEmpleadoAsignado().getId() : null)
                .empleadoAsignadoNombre(c.getEmpleadoAsignado() != null ? c.getEmpleadoAsignado().getNombre() : null)
                .empleadoPreferidoId(c.getEmpleadoPreferido() != null ? c.getEmpleadoPreferido().getId() : null)
                .empleadoPreferidoNombre(c.getEmpleadoPreferido() != null ? c.getEmpleadoPreferido().getNombre() : null)
                .fechaHoraInicio(c.getFechaHoraInicio())
                .fechaHoraFin(c.getFechaHoraFin())
                .precioFinal(c.getPrecioFinal())
                .recargoPorcentaje(c.getRecargoPorcentaje() != null ? c.getRecargoPorcentaje() : BigDecimal.ZERO)
                .metodoPago(c.getMetodoPago())
                .penalizacionCliente(c.getCliente() != null && c.getCliente().getPenalizacionPorcentaje() != null
                        ? c.getCliente().getPenalizacionPorcentaje() : BigDecimal.ZERO)
                .motivoCancelacion(c.getMotivoCancelacion())
                .notas(c.getNotas())
                .creadoEn(c.getCreadoEn())
                .build();
    }
}
