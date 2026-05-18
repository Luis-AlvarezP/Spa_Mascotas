package com.spamascotas.spa_mascotas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClienteAdminResponse {
    private Long id;
    private Long usuarioId;
    private String correo;
    private String nombre;
    private String ci;
    private String telefono;
    private String direccion;
    private String estado;
    private boolean totpHabilitado;
    private List<String> mascotas;
    private Map<String, String> preferencias;
}
