package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VentaItemResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoUrlImagen;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotalItem;
}
