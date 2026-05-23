package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteInfoResponse {
    private int descuentoPct;
    private long pedidosEntregados;
    private int pedidosParaSiguienteNivel;
    private BigDecimal penalizacionPct;
}
