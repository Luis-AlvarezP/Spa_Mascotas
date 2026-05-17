package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStaffRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String ci;

    private String telefono;
}
