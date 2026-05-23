package com.spamascotas.spa_mascotas_api.service.recepcion;

import com.spamascotas.spa_mascotas_api.dto.response.RecepcionDashboardResponse;
import com.spamascotas.spa_mascotas_api.model.Cita;
import com.spamascotas.spa_mascotas_api.model.Venta;
import com.spamascotas.spa_mascotas_api.repository.CitaRepository;
import com.spamascotas.spa_mascotas_api.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RecepcionDashboardService {

    private final CitaRepository citaRepo;
    private final VentaRepository ventaRepo;

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public RecepcionDashboardResponse getDashboard() {
        ZoneId bolivia = ZoneId.of("America/La_Paz");
        LocalDate hoy = LocalDate.now(bolivia);
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay();

        List<RecepcionDashboardResponse.CitaHoyDto> cronograma =
                citaRepo.findCronogramaHoy(inicio, fin).stream().map(this::toCitaDto).toList();

        List<RecepcionDashboardResponse.CitaHoyDto> canceladas =
                citaRepo.findCanceladasHoy(inicio, fin).stream().map(this::toCitaDto).toList();

        return RecepcionDashboardResponse.builder()
                .cronograma(cronograma)
                .canceladas(canceladas)
                .caja(buildCaja(inicio, fin))
                .build();
    }

    private RecepcionDashboardResponse.CajaDiariaDto buildCaja(LocalDateTime inicio, LocalDateTime fin) {
        List<Cita>  realizadas = citaRepo.findRealizadasHoy(inicio, fin);
        List<Venta> ventas     = ventaRepo.findVentasEntre(inicio, fin);

        List<RecepcionDashboardResponse.MovimientoCajaDto> movimientos = new ArrayList<>();
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        BigDecimal totalServicios = BigDecimal.ZERO;
        BigDecimal totalProductos = BigDecimal.ZERO;

        for (Cita c : realizadas) {
            String metodo = c.getMetodoPago() != null ? c.getMetodoPago() : "SIN ESPECIFICAR";
            BigDecimal recargo = c.getRecargoPorcentaje() != null ? c.getRecargoPorcentaje() : BigDecimal.ZERO;
            BigDecimal base = c.getPrecioFinal() != null ? c.getPrecioFinal() : BigDecimal.ZERO;
            BigDecimal total = recargo.compareTo(BigDecimal.ZERO) > 0
                    ? base.add(base.multiply(recargo).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                    : base;

            totalServicios = totalServicios.add(total);
            porMetodo.merge(metodo, total, BigDecimal::add);

            movimientos.add(RecepcionDashboardResponse.MovimientoCajaDto.builder()
                    .tipo("SERVICIO")
                    .descripcion(c.getServicio() != null ? c.getServicio().getNombre() : "—")
                    .clienteNombre(c.getCliente() != null ? c.getCliente().getNombre() : "—")
                    .clienteCi(c.getCliente() != null ? c.getCliente().getCi() : "—")
                    .metodoPago(metodo)
                    .monto(total.setScale(2, RoundingMode.HALF_UP))
                    .hora(c.getFechaHoraInicio() != null ? c.getFechaHoraInicio().format(HORA_FMT) : "—")
                    .build());
        }

        for (Venta v : ventas) {
            String metodo = v.getMetodoPago() != null ? v.getMetodoPago().getNombre() : "SIN ESPECIFICAR";
            BigDecimal total = v.getTotalFinal() != null ? v.getTotalFinal() : BigDecimal.ZERO;

            totalProductos = totalProductos.add(total);
            porMetodo.merge(metodo, total, BigDecimal::add);

            movimientos.add(RecepcionDashboardResponse.MovimientoCajaDto.builder()
                    .tipo("PRODUCTO")
                    .descripcion("Venta de productos #" + v.getId())
                    .clienteNombre(v.getCliente() != null ? v.getCliente().getNombre() : "—")
                    .clienteCi(v.getCliente() != null ? v.getCliente().getCi() : "—")
                    .metodoPago(metodo)
                    .monto(total.setScale(2, RoundingMode.HALF_UP))
                    .hora(v.getFechaVenta() != null ? v.getFechaVenta().format(HORA_FMT) : "—")
                    .build());
        }

        movimientos.sort(Comparator.comparing(RecepcionDashboardResponse.MovimientoCajaDto::getHora));

        porMetodo.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_UP));

        return RecepcionDashboardResponse.CajaDiariaDto.builder()
                .totalServicios(totalServicios.setScale(2, RoundingMode.HALF_UP))
                .totalProductos(totalProductos.setScale(2, RoundingMode.HALF_UP))
                .totalGeneral(totalServicios.add(totalProductos).setScale(2, RoundingMode.HALF_UP))
                .porMetodoPago(porMetodo)
                .movimientos(movimientos)
                .build();
    }

    private RecepcionDashboardResponse.CitaHoyDto toCitaDto(Cita c) {
        String groomer = c.getEmpleadoAsignado() != null ? c.getEmpleadoAsignado().getNombre()
                : (c.getEmpleadoPreferido() != null ? c.getEmpleadoPreferido().getNombre() : "Por asignar");
        return RecepcionDashboardResponse.CitaHoyDto.builder()
                .citaId(c.getId())
                .clienteNombre(c.getCliente() != null ? c.getCliente().getNombre() : "—")
                .clienteCi(c.getCliente() != null ? c.getCliente().getCi() : "—")
                .mascotaNombre(c.getMascota() != null ? c.getMascota().getNombre() : "—")
                .mascotaEspecie(c.getMascota() != null ? c.getMascota().getEspecie() : null)
                .mascotaTamano(c.getMascota() != null ? c.getMascota().getTamano() : null)
                .mascotaTemperamento(c.getMascota() != null && c.getMascota().getTemperamento() != null
                        ? c.getMascota().getTemperamento().getNombre() : null)
                .servicio(c.getServicio() != null ? c.getServicio().getNombre() : "—")
                .groomer(groomer)
                .horaInicio(c.getFechaHoraInicio() != null ? c.getFechaHoraInicio().format(HORA_FMT) : "—")
                .horaFin(c.getFechaHoraFin()   != null ? c.getFechaHoraFin().format(HORA_FMT)   : "—")
                .estado(c.getEstado())
                .motivoCancelacion(c.getMotivoCancelacion())
                .build();
    }
}
