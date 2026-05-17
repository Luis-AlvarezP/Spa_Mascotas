package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioTrabajoResponse {
    private Long id;
    private Long empleadoId;
    private String empleadoNombre;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private LocalTime inicioAlmuerzo;
    private LocalTime finAlmuerzo;
    private LocalDate vigenteDesde;
    private LocalDate vigenteHasta;
    private int capacidadMaxima;
}
