package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Enable2faRequest {
    @NotBlank private String correo;
    @NotBlank private String password;
    private int codigo;
}
