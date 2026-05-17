package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bloqueos_agenda")
public class BloqueoAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bloqueos_gen")
    @SequenceGenerator(name = "bloqueos_gen", sequenceName = "bloqueos_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    private String tipo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;
}
