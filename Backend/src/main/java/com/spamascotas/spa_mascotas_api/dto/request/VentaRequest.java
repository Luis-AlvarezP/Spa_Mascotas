package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VentaRequest {
    private Long clienteId;           // null = propio cliente; requerido para RECEPCION
    @NotNull private Long metodoPagoId;
    private String codigoCupon;
    private BigDecimal descuentoManual; // solo RECEPCION
    @NotEmpty private List<VentaItemRequest> items;
    private String telefonoContacto;    // si cliente no tiene teléfono, se guarda
    private String tipoEntrega;         // RECOGER (default) | DOMICILIO
    private String direccionEntrega;    // requerido si tipoEntrega=DOMICILIO
}
