package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "promociones")
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "promociones_gen")
    @SequenceGenerator(name = "promociones_gen", sequenceName = "promociones_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private String tipo = "TEMPORADA";

    @Column(name = "descuento_porcentaje", nullable = false)
    @Builder.Default
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "promo_productos",
        joinColumns = @JoinColumn(name = "promo_id"),
        inverseJoinColumns = @JoinColumn(name = "producto_id"))
    @Builder.Default
    private Set<Producto> productos = new HashSet<>();
}
