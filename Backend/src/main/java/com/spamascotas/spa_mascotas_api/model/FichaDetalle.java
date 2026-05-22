package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ficha_detalles")
public class FichaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ficha_detalles_gen")
    @SequenceGenerator(name = "ficha_detalles_gen", sequenceName = "ficha_detalles_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ficha_id")
    private FichaIngreso ficha;

    @Column(name = "descripcion_estado", columnDefinition = "TEXT")
    private String descripcionEstado;
}
