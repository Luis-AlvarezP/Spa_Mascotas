package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SlotResponse {
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private boolean disponible;
    private List<GroomerInfo> groomers;

    @Data
    @Builder
    public static class GroomerInfo {
        private Long id;
        private String nombre;
    }
}
