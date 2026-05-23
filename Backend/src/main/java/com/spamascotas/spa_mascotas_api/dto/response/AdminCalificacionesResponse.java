package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminCalificacionesResponse {

    List<GroomerStatsDto> groomers;
    List<CalificacionDto> calificaciones;
    BigDecimal promedioGeneral;
    Long totalCalificaciones;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GroomerStatsDto {
        Long id;
        String nombre;
        BigDecimal promedio;
        Long total;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CalificacionDto {
        Long id;
        String groomer;
        Integer puntuacion;
        String comentario;
        String fecha;
        String mascota;
        String servicio;
        String cliente;
    }
}
