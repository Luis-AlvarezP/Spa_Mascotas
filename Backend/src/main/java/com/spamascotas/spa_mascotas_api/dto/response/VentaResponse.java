package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VentaResponse {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private String clienteCi;
    private String clienteTelefono;
    private String clienteCanal;
    private String vendedorNombre;
    private String metodoPago;
    private String codigoCupon;
    private LocalDateTime fechaVenta;
    private BigDecimal subtotal;
    private BigDecimal descuentoCupon;
    private BigDecimal descuentoFrecuente;
    private BigDecimal descuentoManual;
    private BigDecimal descuento;
    private BigDecimal totalFinal;
    private String estado;
    private String notas;
    private boolean clienteFrecuente;
    private List<VentaItemResponse> items;
    private Long pedidoId;
    private String estadoPedido;
    private String tipoEntrega;
    private String direccionEntrega;
}
