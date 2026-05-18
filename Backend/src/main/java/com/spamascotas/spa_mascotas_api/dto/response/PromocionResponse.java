package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PromocionResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private String tipo;
    private BigDecimal descuentoPorcentaje;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean activa;
    private List<Long> productoIds;
    private boolean aplicaATodo;
}
