package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CuponResponse {
    private Long id;
    private String codigo;
    private String descripcion;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal descuentoFijo;
    private Integer usosMax;
    private Integer usosActuales;
    private LocalDate fechaVencimiento;
    private Boolean activo;
    private List<Long> productoIds;
}
