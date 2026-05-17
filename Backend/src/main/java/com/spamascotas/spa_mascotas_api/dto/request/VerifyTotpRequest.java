package com.spamascotas.spa_mascotas_api.dto.request;

import lombok.Data;

@Data
public class VerifyTotpRequest {
    private int codigo;
}
