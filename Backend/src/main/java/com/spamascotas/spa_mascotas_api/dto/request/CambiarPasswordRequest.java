package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarPasswordRequest {
    @NotBlank
    private String passwordActual;

    @NotBlank
    @Size(min = 8)
    private String nuevaPassword;
}
