package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "restricciones_mascota")
public class RestriccionMascota {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "restricciones_gen")
    @SequenceGenerator(name = "restricciones_gen", sequenceName = "restricciones_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mascota_id")
    private Mascota mascota;

    private String tipo;
    private String descripcion;
    private String gravedad;
}
