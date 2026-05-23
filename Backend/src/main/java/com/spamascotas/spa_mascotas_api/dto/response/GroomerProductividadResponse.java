package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroomerProductividadResponse {

    private int totalServicios;
    private List<ServicioHoyDto> servicios;

    private int totalInsumosUnidades;
    private List<InsumoHoyDto> insumos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioHoyDto {
        private Long citaId;
        private String mascota;
        private String servicio;
        private String horaInicio;
        private String horaFin;
        private BigDecimal precio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsumoHoyDto {
        private String producto;
        private Integer cantidad;
        private String estado;
        private String mascota;
        private String hora;
    }
}
