package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class HorarioTrabajoRequest {

    @NotNull
    private Long empleadoId;

    @NotBlank
    private String diaSemana;

    @NotBlank
    private String horaInicio;

    @NotBlank
    private String horaFin;

    private String inicioAlmuerzo;

    private String finAlmuerzo;

    @NotBlank
    private String vigenteDesde;

    private String vigenteHasta;

    @Min(1) @Max(20)
    private int capacidadMaxima = 8;
}
