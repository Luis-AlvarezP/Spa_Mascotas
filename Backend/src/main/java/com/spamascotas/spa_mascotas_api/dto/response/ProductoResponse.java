package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ProductoResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private String sku;
    private Integer stockActual;
    private Integer stockMinimo;
    private BigDecimal precioVenta;
    private LocalDate fechaVencimiento;
    private String lote;
    private String urlImagen;
    private Boolean activo;
    private Long categoriaId;
    private String categoriaNombre;
}
