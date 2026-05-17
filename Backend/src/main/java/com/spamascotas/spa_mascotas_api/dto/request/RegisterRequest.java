package com.spamascotas.spa_mascotas_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).+$",
        message = "La contraseña debe incluir mayúsculas, minúsculas, números y símbolos"
    )
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String nombreUsuario;
    private String telefono;
    private String ci;
    private String direccion;
}
