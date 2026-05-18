package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetodoPagoResponse {
    private Long id;
    private String nombre;
}
