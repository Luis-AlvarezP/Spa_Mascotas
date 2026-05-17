package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "horarios_trabajo")
public class HorarioTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "horarios_gen")
    @SequenceGenerator(name = "horarios_gen", sequenceName = "horarios_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    @Column(name = "dia_semana", nullable = false)
    private String diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "inicio_almuerzo")
    private LocalTime inicioAlmuerzo;

    @Column(name = "fin_almuerzo")
    private LocalTime finAlmuerzo;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Builder.Default
    @Column(name = "capacidad_maxima")
    private Integer capacidadMaxima = 8;
}
