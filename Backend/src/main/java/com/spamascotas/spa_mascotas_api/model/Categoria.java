package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categoria_gen")
    @SequenceGenerator(name = "categoria_gen", sequenceName = "categoria_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "nom_categoria", nullable = false)
    private String nombre;

    private String descripcion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;
}
