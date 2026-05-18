package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PromocionRequest {
    @NotBlank private String nombre;
    private String descripcion;
    @NotBlank private String tipo;
    @NotNull  private BigDecimal descuentoPorcentaje;
    @NotNull  private LocalDate fechaInicio;
    @NotNull  private LocalDate fechaFin;
    private List<Long> productoIds; // null o vacío = aplica a todos
}
