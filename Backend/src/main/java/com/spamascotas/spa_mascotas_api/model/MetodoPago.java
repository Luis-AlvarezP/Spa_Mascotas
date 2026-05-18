package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metodos_pago")
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metodos_pago_gen")
    @SequenceGenerator(name = "metodos_pago_gen", sequenceName = "metodos_pago_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String nombre;
}
