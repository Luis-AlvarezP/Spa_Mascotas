package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductoRequest {
    @NotBlank private String nombre;
    private String descripcion;
    private String sku;
    @NotNull private BigDecimal precioVenta;
    private Integer stockActual;
    private Integer stockMinimo;
    private LocalDate fechaVencimiento;
    private String lote;
    private Long categoriaId;
}
