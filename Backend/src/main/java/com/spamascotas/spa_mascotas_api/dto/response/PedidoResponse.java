package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PedidoResponse {
    private Long id;
    private Long ventaId;
    private String tipoEntrega;
    private String direccionEntrega;
    private String estado;
    private LocalDateTime fechaEntregaPedido;
    private LocalDateTime fechaVenta;
    private String clienteNombre;
    private String clienteCi;
    private String clienteTelefono;
    private BigDecimal totalFinal;
    private String itemsResumen;
}
