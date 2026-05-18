package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CuponValidadoResponse {
    private Long cuponId;
    private String codigo;
    private String descripcion;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal descuentoFijo;
    private BigDecimal montoDescuento;
    private boolean aplicaATodo;
}
