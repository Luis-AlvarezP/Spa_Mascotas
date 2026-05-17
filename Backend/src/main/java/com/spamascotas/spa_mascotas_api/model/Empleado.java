package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "empleados")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "empleados_gen")
    @SequenceGenerator(name = "empleados_gen", sequenceName = "empleados_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String nombre;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private String puesto;

    private String ci;

    private String telefono;
}
