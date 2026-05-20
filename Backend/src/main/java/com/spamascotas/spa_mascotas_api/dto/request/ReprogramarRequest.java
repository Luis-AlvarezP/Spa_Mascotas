package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReprogramarRequest {
    @NotBlank
    private String fechaHoraInicio;
    private String notas;
    private Long empleadoPreferidoId;
}
