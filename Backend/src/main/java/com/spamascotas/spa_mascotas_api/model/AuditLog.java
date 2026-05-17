package com.spamascotas.spa_mascotas_api.model;

import com.spamascotas.spa_mascotas_api.model.enums.TipoAccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_logs_gen")
    @SequenceGenerator(name = "audit_logs_gen", sequenceName = "audit_logs_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoAccion accion;

    @Column(name = "correo_usuario", length = 255)
    private String correoUsuario;

    @Column(length = 20)
    private String rol;

    @Column(length = 60)
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String detalles;

    @Column(nullable = false)
    private boolean exitoso;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public String getAccionLabel() {
        return accion != null ? accion.getLabel() : null;
    }
}
