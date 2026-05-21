package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CitaResponse {
    private Long id;
    private String estado;

    private Long clienteId;
    private String clienteNombre;
    private String clienteCorreo;
    private String clienteCi;

    private Long mascotaId;
    private String mascotaNombre;
    private String mascotaEspecie;
    private String mascotaTamano;
    private String mascotaTemperamento;
    private String mascotaTemperamentoColor;

    private Long servicioId;
    private String servicioNombre;
    private Integer duracionMinutos;

    private Long empleadoAsignadoId;
    private String empleadoAsignadoNombre;
    private Long empleadoPreferidoId;
    private String empleadoPreferidoNombre;

    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private BigDecimal precioFinal;
    private BigDecimal recargoPorcentaje;
    private String metodoPago;
    private BigDecimal penalizacionCliente;
    private String motivoCancelacion;
    private String notas;
    private LocalDateTime creadoEn;
}
