package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoriaRequest {
    @NotBlank private String nombre;
    private String descripcion;
}
