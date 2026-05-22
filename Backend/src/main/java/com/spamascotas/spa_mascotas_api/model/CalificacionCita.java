package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "calificaciones_cita")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CalificacionCita {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "calificaciones_seq")
    @SequenceGenerator(name = "calificaciones_seq", sequenceName = "calificaciones_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    private Integer puntuacion;
    private String comentario;
    private LocalDateTime fecha;
}
