package com.spamascotas.spa_mascotas_api.model;

import com.spamascotas.spa_mascotas_api.model.enums.EstadoUsuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuarios_gen")
    @SequenceGenerator(name = "usuarios_gen", sequenceName = "usuarios_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String correo;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.PENDIENTE;

    @Column(name = "token_activacion")
    private String tokenActivacion;

    @Column(name = "token_activacion_exp")
    private LocalDateTime tokenActivacionExp;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_habilitado", nullable = false)
    private Boolean totpHabilitado = false;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    @Column(name = "nombre_usuario", unique = true, length = 50)
    private String nombreUsuario;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_exp")
    private LocalDateTime resetTokenExp;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    public boolean estaBloqueado() {
        return bloqueadoHasta != null && LocalDateTime.now().isBefore(bloqueadoHasta);
    }
}
