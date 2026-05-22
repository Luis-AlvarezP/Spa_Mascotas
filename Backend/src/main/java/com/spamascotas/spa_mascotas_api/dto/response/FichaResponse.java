package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FichaResponse {
    private Long id;
    private Long citaId;
    private String mascotaNombre;
    private String clienteNombre;
    private String servicioNombre;
    private String fechaCita;
    private String estadoIngreso;
    private String observacionesGenerales;
    private List<String> detalles;
    private Boolean checklistUnas;
    private Boolean checklistOidos;
    private Boolean checklistGlandulas;
    private Boolean checklistCorte;
    private Boolean checklistBano;
    private Boolean checklistPerfume;
    private int checklistCompletados;
    private String estado;
    private LocalDateTime creadoEn;
    private String recomendacion;
    private String proximaCitaSugerida;
    private List<FotoResponse> fotos;

    @Data
    @Builder
    public static class FotoResponse {
        private Long id;
        private String url;
        private String momento;
    }
}
