package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class CalificacionResponse {
    private Long id;
    private Long citaId;
    private Integer puntuacion;
    private String comentario;
    private LocalDateTime fecha;
}
