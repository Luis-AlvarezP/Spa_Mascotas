package com.spamascotas.spa_mascotas_api.dto.request;

import lombok.Data;

@Data
public class PerfilUpdateRequest {
    private String nombre;
    private String telefono;
    private String ci;
    private String nombreUsuario;
}
