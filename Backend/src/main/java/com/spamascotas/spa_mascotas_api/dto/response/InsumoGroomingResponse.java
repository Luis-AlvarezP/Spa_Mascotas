package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InsumoGroomingResponse {
    private Long id;
    private Long citaId;
    private String mascotaNombre;
    private String servicioNombre;
    private Long empleadoId;
    private String empleadoNombre;
    private Long productoId;
    private String productoNombre;
    private Integer cantidad;
    private String estado;
    private String notas;
    private LocalDateTime fecha;
}
