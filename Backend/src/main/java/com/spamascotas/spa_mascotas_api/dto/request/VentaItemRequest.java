package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VentaItemRequest {
    @NotNull private Long productoId;
    @NotNull @Min(1) private Integer cantidad;
}
