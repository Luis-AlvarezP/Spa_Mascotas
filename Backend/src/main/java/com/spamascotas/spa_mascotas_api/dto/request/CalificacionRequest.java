package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CalificacionRequest {
    @NotNull private Long citaId;
    @NotNull @Min(1) @Max(5) private Integer puntuacion;
    private String comentario;
}
