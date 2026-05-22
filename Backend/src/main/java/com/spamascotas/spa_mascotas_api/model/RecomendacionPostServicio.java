package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recomendaciones_post_servicio")
public class RecomendacionPostServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recomendaciones_gen")
    @SequenceGenerator(name = "recomendaciones_gen", sequenceName = "recomendaciones_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @Column(columnDefinition = "TEXT")
    private String recomendacion;

    @Column(name = "proxima_cita_sugerida")
    private LocalDate proximaCitaSugerida;
}
