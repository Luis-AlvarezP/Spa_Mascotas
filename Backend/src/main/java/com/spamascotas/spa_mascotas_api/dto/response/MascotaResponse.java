package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MascotaResponse {
    private Long id;
    private String nombre;
    private String especie;
    private String raza;
    private String tamano;
    private LocalDate fechaNacimiento;
    private String alergias;
    private Long temperamentoId;
    private String temperamentoNombre;
    private String temperamentoColor;
    private String urlFotoMascota;
    private String urlCarnet;
    private Boolean activa;
    private List<RestriccionResponse> restricciones;
}
