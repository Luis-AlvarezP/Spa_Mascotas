package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.*;
import java.util.List;

@Data @Builder
public class HistorialCitaResponse {
    private Long citaId;
    private String clienteNombre;
    private String clienteCi;
    private String mascotaNombre;
    private String mascotaEspecie;
    private String mascotaTamano;
    private String mascotaFotoUrl;
    private String servicio;
    private String groomer;
    private String fechaHoraInicio;
    private String fechaHoraFin;
    private Double precioFinal;
    private String metodoPago;
    private String estadoIngreso;
    private String temperamento;
    private String notasCita;
    private String observaciones;
    private Boolean checklistBano;
    private Boolean checklistCorte;
    private Boolean checklistUnas;
    private Boolean checklistOidos;
    private Boolean checklistGlandulas;
    private Boolean checklistPerfume;
    private Boolean checklistPeinado;
    private List<String> detalles;
    private List<FichaResponse.FotoResponse> fotos;
    private String recomendacion;
    private String proximaCitaSugerida;
    private CalificacionResponse calificacion;
}
