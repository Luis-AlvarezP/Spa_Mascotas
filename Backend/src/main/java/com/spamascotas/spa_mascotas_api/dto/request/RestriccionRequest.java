package com.spamascotas.spa_mascotas_api.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestriccionRequest {
    private String tipo;
    private String descripcion;
    private String gravedad;
}
