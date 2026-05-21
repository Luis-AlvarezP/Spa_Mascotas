package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CobrarRequest {
    @NotBlank
    private String metodoPago;
}
