package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clientes_gen")
    @SequenceGenerator(name = "clientes_gen", sequenceName = "clientes_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String nombre;

    private String ci;

    private String telefono;

    private String direccion;

    @Column(name = "penalizacion_porcentaje", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal penalizacionPorcentaje = java.math.BigDecimal.ZERO;
}
