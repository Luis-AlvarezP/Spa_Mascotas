package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MascotaRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String especie;

    private String raza;

    @NotBlank
    private String tamano;

    private String fechaNacimiento;

    private String alergias;

    private Long temperamentoId;

    private List<RestriccionRequest> restricciones = new ArrayList<>();
}
