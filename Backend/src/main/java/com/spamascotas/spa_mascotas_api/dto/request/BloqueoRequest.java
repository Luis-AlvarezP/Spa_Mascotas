package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BloqueoRequest {

    @NotBlank
    private String titulo;

    private String tipo;

    @NotBlank
    private String fechaInicio;

    @NotBlank
    private String fechaFin;

    private Long empleadoId;

    private String descripcion;
}
