package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilResponse {
    private String correo;
    private String nombre;
    private String nombreUsuario;
    private String rol;
    private String ci;
    private String telefono;
    private boolean totpHabilitado;
    private boolean tieneGoogle;
}
