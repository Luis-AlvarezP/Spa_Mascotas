package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cat_temperamentos")
public class CatTemperamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cat_temp_gen")
    @SequenceGenerator(name = "cat_temp_gen", sequenceName = "cat_temp_id_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nombre;

    @Column(name = "color_alerta")
    private String colorAlerta;
}
