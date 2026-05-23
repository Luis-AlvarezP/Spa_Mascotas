package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminVentasResponse {

    BigDecimal totalServicios;
    BigDecimal totalProductos;
    BigDecimal totalGeneral;
    Map<String, BigDecimal> porMetodoPago;
    List<MovimientoDto> movimientos;
    List<RankingItemDto> rankingServicios;
    List<RankingItemDto> rankingProductos;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MovimientoDto {
        String fecha;
        String tipo;
        String descripcion;
        String clienteNombre;
        String clienteCi;
        String metodoPago;
        BigDecimal monto;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RankingItemDto {
        String nombre;
        Long cantidad;
        BigDecimal totalIngresos;
    }
}
