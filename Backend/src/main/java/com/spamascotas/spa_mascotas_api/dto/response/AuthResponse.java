package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String correo;
    private String nombreUsuario;
    private String rol;
    private boolean requiere2fa;
    private boolean requiere2faSetup;
    private boolean totpHabilitado;
}
