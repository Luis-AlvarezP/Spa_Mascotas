package com.spamascotas.spa_mascotas_api.dto.request;

import lombok.Data;

@Data
public class InsumoGroomingRequest {
    private Long citaId;
    private Long empleadoId;
    private Long productoId;
    private Integer cantidad;
    private String notas;
}
