package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "galeria_mascotas")
public class GaleriaMascota {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "galeria_gen")
    @SequenceGenerator(name = "galeria_gen", sequenceName = "galeria_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @Column(name = "url_image", nullable = false, columnDefinition = "TEXT")
    private String urlImage;

    private String momento;

    @Builder.Default
    @Column(name = "fecha_carga")
    private LocalDateTime fechaCarga = LocalDateTime.now();
}
