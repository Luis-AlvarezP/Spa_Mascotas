package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecepcionDashboardResponse {

    List<CitaHoyDto> cronograma;
    List<CitaHoyDto> canceladas;
    CajaDiariaDto    caja;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CitaHoyDto {
        Long   citaId;
        String clienteNombre;
        String clienteCi;
        String mascotaNombre;
        String mascotaEspecie;
        String mascotaTamano;
        String mascotaTemperamento;
        String servicio;
        String groomer;
        String horaInicio;
        String horaFin;
        String estado;
        String motivoCancelacion;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CajaDiariaDto {
        BigDecimal              totalServicios;
        BigDecimal              totalProductos;
        BigDecimal              totalGeneral;
        Map<String, BigDecimal> porMetodoPago;
        List<MovimientoCajaDto> movimientos;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MovimientoCajaDto {
        String     tipo;
        String     descripcion;
        String     clienteNombre;
        String     clienteCi;
        String     metodoPago;
        BigDecimal monto;
        String     hora;
    }
}
