package com.spamascotas.spa_mascotas_api.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class FichaRequest {
    private Long citaId;
    private String estadoIngreso;
    private String observacionesGenerales;
    private List<String> detalles;
    private Boolean checklistUnas;
    private Boolean checklistOidos;
    private Boolean checklistGlandulas;
    private Boolean checklistCorte;
    private Boolean checklistBano;
    private Boolean checklistPerfume;
    private String recomendacion;
    private String proximaCitaSugerida;
}
