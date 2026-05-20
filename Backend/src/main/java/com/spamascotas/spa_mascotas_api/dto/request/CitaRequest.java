package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CitaRequest {
    @NotNull
    private Long mascotaId;
    @NotNull
    private Long servicioId;
    private Long empleadoPreferidoId;
    @NotBlank
    private String fechaHoraInicio;
    private String notas;
}
