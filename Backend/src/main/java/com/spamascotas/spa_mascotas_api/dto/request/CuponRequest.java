package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CuponRequest {
    @NotBlank private String codigo;
    private String descripcion;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal descuentoFijo;
    private Integer usosMax;
    private LocalDate fechaVencimiento;
    private List<Long> productoIds;
}
