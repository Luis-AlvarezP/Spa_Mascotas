package com.spamascotas.spa_mascotas_api.service.admin;

import com.spamascotas.spa_mascotas_api.dto.response.AdminCalificacionesResponse;
import com.spamascotas.spa_mascotas_api.dto.response.AdminVentasResponse;
import com.spamascotas.spa_mascotas_api.model.CalificacionCita;
import com.spamascotas.spa_mascotas_api.model.Cita;
import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.model.Venta;
import com.spamascotas.spa_mascotas_api.repository.CalificacionCitaRepository;
import com.spamascotas.spa_mascotas_api.repository.CitaRepository;
import com.spamascotas.spa_mascotas_api.repository.EmpleadoRepository;
import com.spamascotas.spa_mascotas_api.repository.VentaItemRepository;
import com.spamascotas.spa_mascotas_api.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportesService {

    private final CitaRepository citaRepo;
    private final VentaRepository ventaRepo;
    private final VentaItemRepository ventaItemRepo;
    private final CalificacionCitaRepository calificacionRepo;
    private final EmpleadoRepository empleadoRepo;

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_SIMPLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public AdminVentasResponse getVentas(LocalDate desde, LocalDate hasta) {
        List<Cita> citas;
        List<Venta> ventas;
        List<Object[]> rankServicios;
        List<Object[]> rankProductos;

        if (desde != null && hasta != null) {
            LocalDateTime ini = desde.atStartOfDay();
            LocalDateTime fin = hasta.plusDays(1).atStartOfDay();
            citas        = citaRepo.findRealizadasHoy(ini, fin);
            ventas       = ventaRepo.findVentasEntre(ini, fin);
            rankServicios = citaRepo.rankingServiciosEntre(ini, fin);
            rankProductos = ventaItemRepo.rankingProductosEntre(ini, fin);
        } else {
            citas         = citaRepo.findTodasRealizadas();
            ventas        = ventaRepo.findAllByOrderByFechaVentaDesc();
            rankServicios = citaRepo.rankingServiciosTodo();
            rankProductos = ventaItemRepo.rankingProductosTodo();
        }

        List<AdminVentasResponse.MovimientoDto> movimientos = new ArrayList<>();
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        BigDecimal totalServicios = BigDecimal.ZERO;
        BigDecimal totalProductos = BigDecimal.ZERO;

        for (Cita c : citas) {
            String metodo = c.getMetodoPago() != null ? c.getMetodoPago() : "SIN ESPECIFICAR";
            BigDecimal recargo = c.getRecargoPorcentaje() != null ? c.getRecargoPorcentaje() : BigDecimal.ZERO;
            BigDecimal base    = c.getPrecioFinal() != null ? c.getPrecioFinal() : BigDecimal.ZERO;
            BigDecimal total   = recargo.compareTo(BigDecimal.ZERO) > 0
                    ? base.add(base.multiply(recargo).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                    : base;
            totalServicios = totalServicios.add(total);
            porMetodo.merge(metodo, total, BigDecimal::add);
            movimientos.add(AdminVentasResponse.MovimientoDto.builder()
                    .fecha(c.getFechaHoraInicio() != null ? c.getFechaHoraInicio().format(FECHA_FMT) : "—")
                    .tipo("SERVICIO")
                    .descripcion(c.getServicio() != null ? c.getServicio().getNombre() : "—")
                    .clienteNombre(c.getCliente() != null ? c.getCliente().getNombre() : "—")
                    .clienteCi(c.getCliente() != null ? c.getCliente().getCi() : "—")
                    .metodoPago(metodo)
                    .monto(total.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        for (Venta v : ventas) {
            String metodo = v.getMetodoPago() != null ? v.getMetodoPago().getNombre() : "SIN ESPECIFICAR";
            BigDecimal total = v.getTotalFinal() != null ? v.getTotalFinal() : BigDecimal.ZERO;
            totalProductos = totalProductos.add(total);
            porMetodo.merge(metodo, total, BigDecimal::add);
            movimientos.add(AdminVentasResponse.MovimientoDto.builder()
                    .fecha(v.getFechaVenta() != null ? v.getFechaVenta().format(FECHA_FMT) : "—")
                    .tipo("PRODUCTO")
                    .descripcion("Venta de productos #" + v.getId())
                    .clienteNombre(v.getCliente() != null ? v.getCliente().getNombre() : "—")
                    .clienteCi(v.getCliente() != null ? v.getCliente().getCi() : "—")
                    .metodoPago(metodo)
                    .monto(total.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        movimientos.sort(Comparator.comparing(AdminVentasResponse.MovimientoDto::getFecha));
        porMetodo.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_UP));

        List<AdminVentasResponse.RankingItemDto> rServicios = rankServicios.stream()
                .limit(10)
                .map(row -> AdminVentasResponse.RankingItemDto.builder()
                        .nombre((String) row[0])
                        .cantidad(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .totalIngresos(row[2] != null
                                ? new BigDecimal(row[2].toString()).setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .toList();

        List<AdminVentasResponse.RankingItemDto> rProductos = rankProductos.stream()
                .limit(10)
                .map(row -> AdminVentasResponse.RankingItemDto.builder()
                        .nombre((String) row[0])
                        .cantidad(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .totalIngresos(row[2] != null
                                ? new BigDecimal(row[2].toString()).setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .toList();

        return AdminVentasResponse.builder()
                .totalServicios(totalServicios.setScale(2, RoundingMode.HALF_UP))
                .totalProductos(totalProductos.setScale(2, RoundingMode.HALF_UP))
                .totalGeneral(totalServicios.add(totalProductos).setScale(2, RoundingMode.HALF_UP))
                .porMetodoPago(porMetodo)
                .movimientos(movimientos)
                .rankingServicios(rServicios)
                .rankingProductos(rProductos)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminCalificacionesResponse getCalificaciones(Long empleadoId) {
        List<Empleado> todosGroomers = empleadoRepo.findByPuesto("GROOMER");
        List<CalificacionCita> todas = calificacionRepo.findAllOrderByFecha();

        List<CalificacionCita> filtradas = empleadoId != null
                ? calificacionRepo.findByEmpleadoId(empleadoId)
                : todas;

        Map<Long, List<CalificacionCita>> byGroomer = todas.stream()
                .filter(cc -> cc.getCita() != null && cc.getCita().getEmpleadoAsignado() != null)
                .collect(Collectors.groupingBy(cc -> cc.getCita().getEmpleadoAsignado().getId()));

        List<AdminCalificacionesResponse.GroomerStatsDto> groomers = todosGroomers.stream()
                .map(emp -> {
                    List<CalificacionCita> grCals = byGroomer.getOrDefault(emp.getId(), List.of());
                    BigDecimal prom = grCals.isEmpty() ? BigDecimal.ZERO
                            : BigDecimal.valueOf(grCals.stream()
                                    .mapToInt(CalificacionCita::getPuntuacion).average().orElse(0))
                              .setScale(2, RoundingMode.HALF_UP);
                    return AdminCalificacionesResponse.GroomerStatsDto.builder()
                            .id(emp.getId())
                            .nombre(emp.getNombre())
                            .promedio(prom)
                            .total((long) grCals.size())
                            .build();
                })
                .sorted(Comparator.comparing(AdminCalificacionesResponse.GroomerStatsDto::getTotal).reversed())
                .toList();

        List<AdminCalificacionesResponse.CalificacionDto> calDtos = filtradas.stream()
                .filter(cc -> cc.getCita() != null)
                .map(cc -> {
                    Cita cita = cc.getCita();
                    return AdminCalificacionesResponse.CalificacionDto.builder()
                            .id(cc.getId())
                            .groomer(cita.getEmpleadoAsignado() != null ? cita.getEmpleadoAsignado().getNombre() : "—")
                            .puntuacion(cc.getPuntuacion())
                            .comentario(cc.getComentario())
                            .fecha(cc.getFecha() != null ? cc.getFecha().format(FECHA_SIMPLE) : "—")
                            .mascota(cita.getMascota() != null ? cita.getMascota().getNombre() : "—")
                            .servicio(cita.getServicio() != null ? cita.getServicio().getNombre() : "—")
                            .cliente(cita.getCliente() != null ? cita.getCliente().getNombre() : "—")
                            .build();
                })
                .toList();

        BigDecimal promedioGeneral = calDtos.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(calDtos.stream()
                        .mapToInt(AdminCalificacionesResponse.CalificacionDto::getPuntuacion)
                        .average().orElse(0))
                  .setScale(2, RoundingMode.HALF_UP);

        return AdminCalificacionesResponse.builder()
                .groomers(groomers)
                .calificaciones(calDtos)
                .promedioGeneral(promedioGeneral)
                .totalCalificaciones((long) calDtos.size())
                .build();
    }
}
